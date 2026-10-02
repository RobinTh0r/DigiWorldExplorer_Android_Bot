# DigiWorldExplorer handoff — 5.2.0 Beta 2

Date: 2026-10-01. This repository is the authoritative source for `v5.2.0-beta.2`. Read this file and `HANDOFF_5.2.0_BETA1.md` before development or device testing.

## Release identity

- App ID: `de.robinthor.digiworldexplorer`
- Version code: 79
- Version name/tag: `5.2.0-beta.2` / `v5.2.0-beta.2`
- Expected signing certificate SHA-256: `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`
- Release APK: `app/build/outputs/release/DigiWorldExplorer-Bot-v5.2.0-beta.2.apk`
- Release APK SHA-256: `0ff486201776e2429bf4228e51f9e255847b3383b260546ee8be09146569ac96`
- Signing files are ignored. Never commit or expose the keystore or `keystore.properties`.

## Digital World profiles

Exactly one persisted profile is active. `V3 Classic` is the original no-Dash/Botamon path. `V4 Dash` is the default and enables the three V4 rules with Botamon recognition and the original three-tap/800 ms rhythm. `V5 All Sprites + Dash (Test)` enables the same Dash rules, generic sprite recognition, all post-5.1.0 phone fixes and the safer two-tap/1.1 s rhythm. Profile changes reset the tracker.

The Samsung S22 safety invariant is global: a zero or unreadable Dash counter authorizes zero Dashes in every profile. Do not restore the historic unknown-counter fallback.

## Beta 2 phone fixes

OnePlus IN2023 offline sessions proved that the former Bond collector could tap the figure below a bubble, open a Partner detail popup and advance without collecting. Bond Rotation now taps only a positively detected bubble, retries while that bubble remains visible and accepts success only after it disappears. There are no blind figure or centre taps.

The translucent Meat Field seed dialog can leave all six plots visible behind it. During an active farm transaction the foreground seed, water or error dialog now takes priority over the background field. Seed counters are read relative to the detected slot panels so device-specific outer padding does not control planting. The existing controller still verifies harvesting, seed selection, confirmation and watering step by step.

Explicit Stop now stops automation and MediaProjection and removes the grid, status and touchable quick-control overlays. Starting again restores the user's saved overlay choices. Diagnostics use up to 50 compressed screenshots per session, larger controls, a solid green active state, readable session timestamps and Share All in both session lists.

## Device constraints

- OnePlus IN2023 / Android 13 closes the game with security error `00000038` while ADB or USB debugging is active. Install first, disconnect and disable debugging, then capture offline diagnostics.
- Samsung S22 Ultra (`SM-S908B`, Android 16) supplied the exhausted-Dash diagnostic evidence.
- Do not claim universal handset compatibility from automated tests alone.
- Ad Skip Pass remains explicit opt-in. Unknown counters never authorize spending.

## Build and release verification

Use Android Studio JBR, Android SDK/build-tools 36.0.0 and Gradle 9.4.1. Run `:app:testDebugUnitTest :app:assembleRelease`, verify the APK certificate and SHA-256, and run `git diff --check`. Preserve app ID, signing identity, settings and installed user data. Future releases still require explicit user authorization.

## Unreleased emulator and cycle follow-up — 2026-10-02

MSI App Player and another community report showed that a red notification marker can open the Digimon section on Buddy or Support Digimon instead of Partner. Bond Rotation now requires the shared Digimon header plus the selected Digimon bottom-navigation tile, then selects the Partner subtab with bounded retries before expanding the roster. Home, Explore and Dungeon fixtures explicitly fail this recovery gate.

VS / Tower Loop is now default-off for an unset preference and has hard gates in both frame orchestration and its analyzer. When disabled, it cannot classify, publish VS status or tap; a user-started Dungeon Co-Pilot can still delegate its own reward result. The quick overlay's remaining German-only labels now use localized resources.

The supplied support ZIP was from 5.1.0 and contained no diagnostic screenshots. Support schema 2 fixes the previously reversed Manual/Co-Pilot label and records the Co-Pilot module switches, cooldown/farm state, DWS phase and explicit Dungeon rotation state for future timer reports.
