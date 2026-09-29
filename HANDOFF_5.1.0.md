# DigiWorldExplorer handoff — 5.1.0

Date: 2026-09-29. This repository is the authoritative source for the stable `v5.1.0` release. Read this file before development or device testing. Older handoffs and repair plans document history and may describe abandoned implementations.

## Release identity

- App ID: `de.robinthor.digiworldexplorer`
- Version code: 77
- Version name/tag: `5.1.0` / `v5.1.0`
- Expected signing certificate SHA-256: `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`
- Release APK: `app/build/outputs/release/DigiWorldExplorer-Bot-v5.1.0.apk`
- Release APK SHA-256: `d601ac0dc40ad040d8b1f9a225b34d8c6421a8d5c84239413562a11c7098babd`
- Signing files are ignored. Never commit or expose `digiworldexplorer-release.jks` or `keystore.properties`.

## What shipped

### Dungeon rotation

Dungeon list/card navigation, Network Defense Matching, result/reward handling and the tall Digi-Factory Ad-Skip sheet use image evidence and normalized component targets. Summon-like screens are excluded from Dungeon classification. OnePlus diagnostics showed two complete rotation runs after these fixes.

### Digi Co-Pilot, Bond and Farm

The OnePlus 1080×2376 Partner page uses a tighter 5×3 grid. The detector verifies all 15 cells, requires the unique green active marker and avoids the lower-right expand control. Bond-bubble recognition uses a wider colour range and taps the associated figure. Meat Field no longer treats `UNKNOWN` plots as completed; field stability ignores volatile exact OCR counts, and an unresolved field requests a diagnostic screenshot.

### Classic Auto-Summon

The final implementation deliberately restores the tracked `v4.0.0` `RewardPurchaseDetector` and `RewardPurchaseFrameAnalyzer`. Only `isSequenceActive()` was added for V5 routing. The V4 path supplies the original fixed footer recognition, 200 ms cadence, five-second sequence window, ten-second Crest window and touch-correction option. Do not reintroduce the abandoned ticket-header detector, state-machine waits or top-centre scheduled bursts without new device evidence.

Classic Auto-Summon is evaluated before generic V5 entry and reveal guards when enabled outside Digi Co-Pilot and Dungeon rotation. This prevents false `ENTRY_CLOSE_NOTICE` taps and stops the reveal guard from starving V4 animation clicks. Supplied OnePlus green-ticket and purple/Crest images are regression fixtures. The user confirmed the final rollback appears to work on the OnePlus device.

### Offline diagnostics

Experimental diagnostic mode works with USB debugging disabled. It retains at most six sessions, each with bounded event logs and up to 50 compressed screenshots. The home screen exposes enable, session list/delete/share and Export All controls. ZIPs contain diagnostics only and are shared through Android's chooser.

## Device and test constraints

- OnePlus IN2023 / Android 13 terminates the game with security error `00000038` when ADB or USB debugging is active. Never run in-game ADB tests on that device. Install/update first, disconnect and disable debugging, then use offline diagnostics.
- Dungeon rotation and final classic Summon were supervised on OnePlus. Samsung S21 Ultra was planned but no final diagnostic proving all flows is recorded here.
- Do not claim compatibility with every handset or every dialog from unit tests alone.
- Ad Skip Pass remains explicit opt-in. Unknown counters never authorize tickets, ads or premium actions.

## Build and verification

Use Android Studio JBR, Android SDK/build-tools 36.0.0 and Gradle 9.4.1. Run `:app:testDebugUnitTest :app:assembleRelease`. Verify the named APK hash, the certificate above and `git diff --check`. Preserve app ID, signing identity, settings and installed user data.

## Working-tree guidance

This release combines the 5.0.3 Beta 2 diagnostics/OnePlus changes with later image-recognition repairs. Treat `HANDOFF_5.0.3_BETA1.md`, `HANDOFF_5.0.3_BETA2.md`, `docs/REPAIR_PLAN_V5_VISION_AND_AUTOMATION.md` and `.local/` diagnostic extracts as historical evidence. Do not develop from older sibling worktrees. Do not publish another release without an explicit user request.
