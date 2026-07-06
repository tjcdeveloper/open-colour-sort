#!/usr/bin/env bash
# Build, sign, and install a version of Open Colour Sort on a connected
# Android device.
#
#   scripts/install-version.sh              list versions and choose
#   scripts/install-version.sh 0.1.5        install a specific version
#   scripts/install-version.sh -f 0.1.5     force rebuild (ignore dist/ cache)
#   scripts/install-version.sh -w           build the current working tree
#                                           (committed or dirty) as-is
#
# Versions are discovered from git history (commits that changed
# versionName in app/build.gradle.kts) and built from that exact commit in
# a temporary git worktree, so the working tree is never touched. With -w
# the build instead runs in place on whatever is on disk right now and the
# APK is suffixed "-dev". APKs are cached in dist/ (gitignored via the
# *.apk rule); -w always rebuilds.
#
# Signing uses the local Android debug keystore - fine for sideloading and
# in-place updates between builds from this machine. A real release
# keystore must replace it before any store publishing.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
KEYSTORE="${KEYSTORE:-$HOME/.android/debug.keystore}"
DIST="$REPO_ROOT/dist"

die() { echo "error: $*" >&2; exit 1; }

[ -x "$ADB" ] || die "adb not found at $ADB (set ANDROID_HOME?)"
[ -d "$SDK/build-tools" ] || die "no build-tools in $SDK"
BUILD_TOOLS="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -1)"
[ -x "$BUILD_TOOLS/apksigner" ] || die "apksigner not found in $BUILD_TOOLS"
[ -f "$KEYSTORE" ] || die "keystore not found at $KEYSTORE"

cd "$REPO_ROOT"

FORCE=0
DIRTY=0
while [ $# -gt 0 ]; do
    case "$1" in
        -f) FORCE=1; shift ;;
        -w) DIRTY=1; shift ;;
        -*) die "unknown flag $1" ;;
        *) break ;;
    esac
done
REQUESTED="${1:-}"
[ "$DIRTY" -eq 1 ] && [ -n "$REQUESTED" ] && die "-w builds the working tree; don't pass a version with it"

if [ "$DIRTY" -eq 1 ]; then
    REQUESTED="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)"
    [ -n "$REQUESTED" ] || die "could not read versionName from app/build.gradle.kts"
    SHA="(working tree)"
fi

# ---------- discover versions: newest first, one line "version<TAB>sha" ----------
VERSIONS=""
seen=" "
if [ "$DIRTY" -eq 0 ]; then
while read -r sha; do
    v=$(git show "$sha:app/build.gradle.kts" 2>/dev/null \
        | sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' | head -1)
    [ -n "$v" ] || continue
    case "$seen" in *" $v "*) continue ;; esac
    seen="$seen$v "
    VERSIONS="${VERSIONS}${v}	${sha}
"
done < <(git log --format=%H -- app/build.gradle.kts)
[ -n "$VERSIONS" ] || die "no versions found in git history"

# ---------- choose a version ----------
if [ -z "$REQUESTED" ]; then
    echo "Available versions (newest first):"
    i=1
    while IFS='	' read -r v sha; do
        [ -n "$v" ] || continue
        printf '  %d) %-8s (%s)\n' "$i" "$v" "$(git show -s --format='%cd %s' --date=short "$sha" | cut -c1-70)"
        i=$((i + 1))
    done <<<"$VERSIONS"
    printf 'Install which version? [1] '
    read -r choice
    choice="${choice:-1}"
    REQUESTED="$(printf '%s' "$VERSIONS" | sed -n "${choice}p" | cut -f1)"
    [ -n "$REQUESTED" ] || die "invalid selection"
fi

SHA="$(printf '%s' "$VERSIONS" | awk -F'	' -v v="$REQUESTED" '$1==v{print $2; exit}')"
[ -n "$SHA" ] || die "version $REQUESTED not in history; available: $(printf '%s' "$VERSIONS" | cut -f1 | tr '\n' ' ')"
fi

# ---------- pick a device: prefer the physical one ----------
# (no mapfile: macOS ships bash 3.2)
DEVICES=()
PHYSICAL=()
while read -r d; do
    [ -n "$d" ] || continue
    DEVICES+=("$d")
    case "$d" in emulator-*) ;; *) PHYSICAL+=("$d") ;; esac
done < <("$ADB" devices | awk 'NR>1 && $2=="device"{print $1}')
if [ "${#PHYSICAL[@]}" -eq 1 ]; then
    DEVICE="${PHYSICAL[0]}"
elif [ "${#PHYSICAL[@]}" -gt 1 ]; then
    echo "Multiple devices attached:"
    i=1
    for d in "${PHYSICAL[@]}"; do
        printf '  %d) %s (%s)\n' "$i" "$d" "$("$ADB" -s "$d" shell getprop ro.product.model | tr -d '\r')"
        i=$((i + 1))
    done
    printf 'Install to which device? [1] '
    read -r choice
    DEVICE="${PHYSICAL[$((${choice:-1} - 1))]:-}"
    [ -n "$DEVICE" ] || die "invalid selection"
elif [ "${#DEVICES[@]}" -ge 1 ] && [ -n "${DEVICES[0]}" ]; then
    DEVICE="${DEVICES[0]}"
    echo "No physical device attached; using ${DEVICE}."
else
    die "no device attached (adb devices is empty)"
fi

# ---------- build (cached in dist/ unless -f; -w always rebuilds in place) ----------
mkdir -p "$DIST"
if [ "$DIRTY" -eq 1 ]; then
    APK="$DIST/open-colour-sort-$REQUESTED-dev.apk"
    echo "Building $REQUESTED from the current working tree..."
    ./gradlew --quiet :app:assembleRelease
    UNSIGNED="app/build/outputs/apk/release/app-release-unsigned.apk"
    [ -f "$UNSIGNED" ] || die "build produced no APK"
    ALIGNED="$(mktemp /tmp/ocs-aligned-XXXXXX.apk)"
    "$BUILD_TOOLS/zipalign" -f -p 4 "$UNSIGNED" "$ALIGNED"
    "$BUILD_TOOLS/apksigner" sign \
        --ks "$KEYSTORE" --ks-pass pass:android --ks-key-alias androiddebugkey \
        --out "$APK" "$ALIGNED" 2>/dev/null
    rm -f "$ALIGNED"
    echo "Built and signed $APK"
elif [ -f "$DIST/open-colour-sort-$REQUESTED.apk" ] && [ "$FORCE" -eq 0 ]; then
    APK="$DIST/open-colour-sort-$REQUESTED.apk"
    echo "Using cached $APK (pass -f to rebuild)."
else
    APK="$DIST/open-colour-sort-$REQUESTED.apk"
    echo "Building $REQUESTED from ${SHA:0:10} in a temporary worktree..."
    WORKTREE="$(mktemp -d /tmp/ocs-build-XXXXXX)"
    cleanup() {
        git -C "$REPO_ROOT" worktree remove --force "$WORKTREE" 2>/dev/null || true
        rm -rf "$WORKTREE"
    }
    trap cleanup EXIT
    git worktree add --detach "$WORKTREE" "$SHA" >/dev/null
    [ -f "$REPO_ROOT/local.properties" ] && cp "$REPO_ROOT/local.properties" "$WORKTREE/"
    (cd "$WORKTREE" && ./gradlew --quiet :app:assembleRelease)
    UNSIGNED="$WORKTREE/app/build/outputs/apk/release/app-release-unsigned.apk"
    [ -f "$UNSIGNED" ] || die "build produced no APK"
    "$BUILD_TOOLS/zipalign" -f -p 4 "$UNSIGNED" "$WORKTREE/aligned.apk"
    "$BUILD_TOOLS/apksigner" sign \
        --ks "$KEYSTORE" --ks-pass pass:android --ks-key-alias androiddebugkey \
        --out "$APK" "$WORKTREE/aligned.apk" 2>/dev/null
    cleanup
    trap - EXIT
    echo "Built and signed $APK"
fi

# ---------- install, handling downgrades ----------
echo "Installing $REQUESTED on $DEVICE..."
if ! OUTPUT="$("$ADB" -s "$DEVICE" install -r "$APK" 2>&1)"; then
    if grep -q "VERSION_DOWNGRADE" <<<"$OUTPUT"; then
        echo "That version is older than the one installed."
        printf 'Uninstall first? This DELETES all progress on the device. [y/N] '
        read -r answer
        [ "${answer:-n}" = "y" ] || die "aborted"
        "$ADB" -s "$DEVICE" uninstall uk.co.tjcdeveloper.opencoloursort >/dev/null
        "$ADB" -s "$DEVICE" install "$APK"
    else
        die "install failed: $OUTPUT"
    fi
fi
"$ADB" -s "$DEVICE" shell dumpsys package uk.co.tjcdeveloper.opencoloursort \
    | grep -m1 versionName | tr -d ' '
echo "Done."
