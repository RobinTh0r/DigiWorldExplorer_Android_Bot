# DigiWorldExplorer handoff — 5.2.0

Date: 2026-10-02. This repository is the authoritative source for stable `v5.2.0`. Read this file and `HANDOFF_5.2.0_BETA2.md` before development or device testing.

## Release identity

- App ID: `de.robinthor.digiworldexplorer`
- Version code: 80
- Version name/tag: `5.2.0` / `v5.2.0`
- Expected signing certificate SHA-256: `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`
- Release APK: `app/build/outputs/release/DigiWorldExplorer-Bot-v5.2.0.apk`
- Release APK SHA-256: `549715688d54af0013ad1881d0c8188a54e3a4cb50b1493dba18b9e96790ba2d`
- Signing files are ignored. Never commit or expose the keystore or `keystore.properties`.

## Stable behavior

Exactly one Digital World profile is active. `V3 Classic` is the original no-Dash/Botamon path. `V4 Dash` is the default and enables the three V4 rules with Botamon recognition and the original three-tap/800 ms rhythm. `V5 All Sprites + Dash (Test)` enables the same Dash rules, generic sprite recognition and the safer two-tap/1.1 s rhythm. A zero or unreadable Dash counter authorizes zero Dashes in every profile.

Bond Rotation taps only positively detected bubbles and advances only after the tapped bubble disappears. Within the verified Digimon section it can recover from Buddy or Support Digimon to Partner before expanding the roster. Meat Field foreground dialogs outrank the still-visible field and seed counters are read relative to detected slot panels.

VS / Tower Loop is default-off for unset preferences and is hard-gated before classification and input. Explicit Stop releases automation, MediaProjection and every app overlay. The quick overlay follows the selected app language. Support schema 2 includes Co-Pilot module switches, cooldown/farm state, DWS phase and explicit Dungeon rotation state.

## Device constraints

- OnePlus IN2023 / Android 13 closes the game with security error `00000038` while ADB or USB debugging is active. Install first, disconnect and disable debugging, then capture offline diagnostics.
- Samsung S22 Ultra (`SM-S908B`, Android 16) supplied the exhausted-Dash diagnostic evidence.
- Do not claim universal handset compatibility from automated tests alone.
- Ad Skip Pass remains explicit opt-in. Unknown counters never authorize spending.

## Build and release verification

Use Android Studio JBR, Android SDK/build-tools 36.0.0 and Gradle 9.4.1. Run `:app:testDebugUnitTest :app:assembleRelease`, verify the APK certificate and SHA-256, and run `git diff --check`. Preserve app ID, signing identity, settings and installed user data. Future releases still require explicit user authorization.

## Unreleased fixes after 5.2.0

- Samsung SM-S948U diagnostics from 2026-10-03 proved that the generic title/login probe
  alternated with an already active Network Defense session. Its false `LOGIN_READY` result sent
  taps to `(540, 1632)`, opening the centre skill card while the Network module was waiting for the
  final boss. An active Network Defense session now excludes the generic entry probe for every
  intermediate, obscured and loading frame. `FrameProbePolicyTest` locks this ownership rule.
- Two separate Discord reports say Dungeon Co-Pilot returns to Home before visually remaining
  dungeons are used. Neither report included a diagnostic session, so do not conflate them with the
  proven Network issue or weaken the counter/settings safety rules speculatively. Request a fresh
  diagnostic ZIP from the start of Dungeon Co-Pilot through the early return; the log now already
  records `DUNGEON.DECISION`, configured/locked cards at start, actions, and completion phase.
