# DigiWorldExplorer handoff — 5.2.0 Beta 1

Date: 2026-09-30. This repository is the authoritative source for `v5.2.0-beta.1`. Read this file and `HANDOFF_5.1.0.md` before development or device testing.

## Release identity

- App ID: `de.robinthor.digiworldexplorer`
- Version code: 78
- Version name/tag: `5.2.0-beta.1` / `v5.2.0-beta.1`
- Expected signing certificate SHA-256: `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`
- Release APK: `app/build/outputs/release/DigiWorldExplorer-Bot-v5.2.0-beta.1.apk`
- Release APK SHA-256: `515f37af57fef5ab265372034b1c2369b5f9d8029f9f47b5d3d028fcab710792`
- Signing files are ignored. Never commit or expose the keystore or `keystore.properties`.

## Digital World profiles

Exactly one persisted profile is active. `V3 Classic` is the original no-Dash/Botamon path. `V4 Dash` is the default and enables the three V4 rules with Botamon recognition and the original three-tap/800 ms rhythm. `V5 All Sprites + Dash (Test)` enables the same Dash rules, generic sprite recognition, all post-5.1.0 phone fixes and the safer two-tap/1.1 s rhythm. Profile changes reset the tracker.

The Samsung S22 safety invariant is global: a zero or unreadable Dash counter authorizes zero Dashes in every profile. Do not restore the historic unknown-counter fallback.

## Included phone fixes

This beta includes the post-5.1.0 physical-device pass for large partner sprites, bounded Partner confirmation retries, Bond collection bursts and fallback taps, Meat Field navigation/actions, Home reward interaction and tall-phone Touch to Start. Classic Auto-Summon remains on the restored 4.0.0 detector and cadence.

## Device constraints

- OnePlus IN2023 / Android 13 closes the game with security error `00000038` while ADB or USB debugging is active. Install first, disconnect and disable debugging, then capture offline diagnostics.
- Samsung S22 Ultra (`SM-S908B`, Android 16) supplied the exhausted-Dash diagnostic evidence.
- Do not claim universal handset compatibility from automated tests alone.
- Ad Skip Pass remains explicit opt-in. Unknown counters never authorize spending.

## Build and release verification

Use Android Studio JBR, Android SDK/build-tools 36.0.0 and Gradle 9.4.1. Run `:app:testDebugUnitTest :app:assembleRelease`, verify the APK certificate and SHA-256, and run `git diff --check`. Preserve app ID, signing identity, settings and installed user data. Future releases still require explicit user authorization.

## Unreleased OnePlus follow-up — 2026-10-01

Two OnePlus IN2023 offline sessions proved that the Bond collector's figure-offset burst and blind centre fallback could open a Partner detail popup and could advance without a confirmed collection. The follow-up taps only a positively detected bubble, retries while it remains visible and accepts success only after the tapped bubble disappears. There are no blind figure or centre taps. Diagnostic fixtures cover the live bubble and the false Partner popup.

The same sessions showed the seed dialog remaining over a still-recognizable Meat Field. Active farm dialogs now take foreground priority over the visible background field, and seed counters are located from the detected slot panels so phone-specific outer padding does not matter. The OnePlus seed dialog is retained as a regression fixture.

Explicit Stop now stops automation and MediaProjection and removes the grid, status and touchable quick-control overlays. Starting again restores the user's saved overlay choices. The Home diagnostic controls no longer include the old support-report button, use larger file/export controls, show a solid green active state, format session dates, and expose Share All inside both diagnostic lists.
