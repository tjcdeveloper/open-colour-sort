# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Open Colour Sort — a free, open-source water/colour-sort puzzle game for Android, sibling app to "Open Twenty Forty-Eight" and sharing its brand. BSD 3-Clause licensed, published by tjcdeveloper.

Kotlin/Jetpack Compose Android app (single `:app` module, package `uk.co.tjcdeveloper.opencoloursort`). Pure Compose foundation — no Material components; all UI is custom-drawn from the design tokens. Min SDK 26, target/compile 36.

## Commands

```bash
./gradlew :app:assembleDebug          # build APK
./gradlew :app:testDebugUnitTest      # run unit tests (JUnit4, JVM)
./gradlew :app:testDebugUnitTest --tests "*SolverTest*"   # one test class

# Regenerate the baked level set after editing PackPlan:
./gradlew :app:testDebugUnitTest --tests "*BakeLevelsTool*" -PbakeLevels=true --rerun-tasks
# Re-measure difficulty distributions (sets PackPlan bands from data):
./gradlew :app:testDebugUnitTest --tests "*Calibrate*" -PbakeLevels=true --rerun-tasks

# Install on the Pixel 10 Pro Fold AVD (emulator-5554):
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tooling notes: no system Gradle — use the wrapper. The Pixel 10 Pro Fold emulator has two displays; screenshot with `adb exec-out screencap -p -d <display-id>` (ids from `dumpsys SurfaceFlinger --display-id`), and change posture with `adb emu posture <1=closed|3=opened>`.

## Architecture

- `game/` — pure Kotlin, fully unit-tested. `Board` (immutable tubes, pour rules), `GameEngine` (session: move count, undo history with 5-undo limit, 1 extra tube), `Solver` (A* optimal length + DFS solvability, both with canonical-state dedup), `LevelGenerator` (seeded shuffle-then-verify).
- `levels/` — `PackPlan` (11-pack difficulty curve: 10 classic packs × 40 levels ramping on colours AND dead-state share, plus the 100-level 12×12 "Final Challenge"; specs are colours / empty tubes / min-moves band / dead-% band), `GeneratedLevels` (BAKED output — never edit by hand, regenerate via BakeLevelsTool), `Packs` (registry + data-driven unlock table: 25% of previous pack through the Easy tier, 50% through Hard, 75% for Extreme 1, 100% for Extreme 2; Final Challenge opens with Intermediate 1 at 25% of Easy 2).
- `data/` — DataStore repositories: `SettingsRepository`, `ProgressRepository` (best-moves per level; solved = has entry).
- `ui/` — `GameViewModel` (level flow, resume at first unsolved), `AppRoot` (Screen enum navigation), adaptive layouts in `GameScreenAdaptive` (cover column vs ≥600dp side-rail; hard boards scroll with edge fades), design tokens in `ui/theme/Tokens.kt`.
- Level encoding everywhere: one string per tube, colour-key chars bottom→top (`GameColour.key`), matching the handoff's script block.

The `tools/` test classes (BakeLevelsTool codegen, plus calibration/analysis utilities like CalibrateViabilityTool and LevelStatsTool) are gated behind `-PbakeLevels=true`, not tests. Difficulty rests on `game/Viability` dead-state analysis — the share of reachable positions from which a win is impossible.

## Hard product constraints

From PRIVACY.md and the brand — these are commitments, not defaults:

- **No data collection of any kind**: no analytics, ads, tracking, or network access. The app requests no permissions.
- Game state (board, scores, settings) is stored on-device only.
- No in-app purchases. Footer copy on every game screen: "Free & open source. No ads, no purchases."

Adding any dependency that phones home, or any permission, violates the privacy policy.

## The design handoff is the source of truth

`design_handoff_open_colour_sort/README.md` is the authoritative spec — read it before any UI or gameplay work. Key facts:

- **High-fidelity**: colors, typography, spacing, and copy are final. Recreate pixel-perfectly; values are CSS px at a 412dp-wide reference, treat px = dp.
- **Target device**: Pixel 10 Pro Fold. Every screen exists for the cover display (412×920); key screens also for the inner unfolded display (840×850). Preferred unfolded layout is 1h (side rail).
- **Dark-first**: true-black `#000000` window background; a light theme is also fully specified. Theme setting is Light/Dark/System.
- **Screens** live in `Open Colour Sort Screens.dc.html` with id badges: 1a–1i (game variants, level select, win dialog, settings, unfolded layouts), 2a–2c (hard mode), 3a–3f (logo history — **3b is the chosen logo**, already applied).
- **Board data is generated in the `<script data-dc-script>` block** at the bottom of that HTML: level encodings (each tube is a string of colour keys, bottom→top), vivid/soft palettes, colorblind symbol mapping, and the seeded hard-mode board generator (12 colours × 12 layers, 16 tubes).
- `android-frame.jsx` and `support.js` are preview scaffolding only — ignore them.
- No external assets: logo, icons, and all art are drawn in code (simple vectors).

### Core model (from the handoff)

- Board: array of tubes, each a stack of colour keys (bottom→top); current level, move count, undo history stack, extra-tubes-remaining.
- Pour rule: tap tube to select, tap second tube to pour if legal (top colour matches and space available). Pour animation ≈250–350ms; haptic on pour (toggleable).
- Settings: theme, colorblindSymbols (bool), haptics (bool), palette (vivid/soft), tubeBottomRadius (4–28dp user setting).
- Progress: per-pack solved levels, best move counts.
- Deliberate UI quirk: **Restart button left, Undo right** — do not "fix" this.
