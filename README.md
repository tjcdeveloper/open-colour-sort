# Open Colour Sort

A free, open-source water/colour-sort puzzle game for Android. Pour liquids
between tubes until every tube holds a single colour. Sibling app to
[Open Twenty Forty-Eight](https://github.com/tjcdeveloper), sharing its brand.

**No ads. No purchases. No tracking. No permissions. 100% fun.**

## The game

- **500 levels across 11 packs** — a Beginner ramp through Easy, Intermediate,
  Hard and Extreme tiers (40 levels each), capped by the 100-level
  **Final Challenge**: 12 colours, 12-deep vials, 16 tubes.
- **Difficulty you can trust**: every level is generated offline and
  solver-verified. The curve runs on two measured ramps — colour count and
  *dead-state share* (the fraction of reachable positions from which a win is
  no longer possible), so difficulty is data, not guesswork.
- **Fair stalemates**: the game proves when no useful move remains and offers
  the extra vial, undo, or restart — never a silent dead end.
- **Foldable-first**: designed for the Pixel 10 Pro Fold with dedicated cover
  and unfolded layouts; adapts to regular phones.
- Dark-first design with a full light theme, colorblind symbols, haptics, and
  adjustable tube styling.

## Privacy

This app collects **no data of any kind** — no analytics, no network access,
no permissions. Game state lives on your device only. See
[PRIVACY.md](PRIVACY.md).

## Building

Requires JDK 17 and the Android SDK (compile SDK 36). No system Gradle needed:

```bash
./gradlew :app:assembleDebug          # build a debug APK
./gradlew :app:testDebugUnitTest      # run the unit tests
```

Debug builds unlock every level for testing; release builds compile that
flag out.

### Level tooling

Levels are baked offline from the difficulty plan in `PackPlan` and frozen
into `GeneratedLevels` (never edit by hand):

```bash
# Regenerate the level set after editing PackPlan:
./gradlew :app:testDebugUnitTest --tests "*BakeLevelsTool*" -PbakeLevels=true --rerun-tasks
# Re-measure difficulty distributions per spec:
./gradlew :app:testDebugUnitTest --tests "*CalibrateViability*" -PbakeLevels=true --rerun-tasks
```

## Licence

[BSD 3-Clause](LICENSE). Published by TJCDeveloper.
