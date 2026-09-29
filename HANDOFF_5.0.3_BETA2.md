# DigiWorldExplorer handoff — 5.0.3 Beta 2

Date: 2026-09-28. This is the current release-ready working copy for `v5.0.3-beta.2`. Version 5.0.1 remains the stable release.

## Device findings and fixes

- A OnePlus 8 Pro on Android 13 terminates the game with security-policy error `00000038` during ADB/UI-automation testing. Do not run the game while ADB or USB debugging is active.
- Experimental settings now contain an opt-in offline diagnostic mode. It stores bounded event logs and targeted compressed screenshots, lists up to six sessions, shares each as a ZIP, and supports individual deletion.
- Supervised OnePlus diagnostics showed that the Digi-Factory Ad-Skip reward uses a tall translucent sheet not covered by the old reward template. Beta 2 recognizes and closes it.
- The expanded OnePlus Partner roster uses tighter row spacing than previous tall-phone fixtures. Beta 2 corrects the 5×3 geometry and verifies all 15 cells before partner selection.
- Real OnePlus reward and Partner screenshots are regression fixtures. Unit tests and the signed release build passed.

## Safety and testing

- Preserve app ID `de.robinthor.digiworldexplorer`, signing identity, settings, and user data.
- Ad Skip Pass remains opt-in. An unreadable counter must never authorize spending.
- Install signed updates with data preservation, turn USB debugging off, enable Global Settings → Experimental → Diagnostic mode, and test Dungeon and Digi Co-Pilot separately.
- Export the resulting ZIP after the game is closed. ADB may be enabled afterward only to retrieve files, then stopped again before another game run.

## Signing

Version code is 76 and version name is `5.0.3-beta.2`. The expected release certificate SHA-256 remains `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`. Signing files are ignored and must never be committed or uploaded.

## Recognition repair implemented 2026-09-29

### Superseding Auto-Summon rollback after diagnostic bundle (4)

User explicitly requested the v4.0.0 implementation verbatim. RewardPurchaseDetector and the
original detector tests were restored from that tag. RewardPurchaseFrameAnalyzer was restored
with only an isSequenceActive routing accessor added. This restores the original 200ms tap
interval, 5-second continuation, 10-second Crest continuation and existing touch correction.
The new ticket-header match, top-centre scheduled bursts and transaction/result waits are no
longer used. Classic enabled Summon runs before V5 entry/reveal guards to prevent interception;
Co-Pilot and active dungeon rotation retain their ownership. The diagnostic screenshot cap of
50 remains. Physical-device validation is still pending; unit/build success is not device proof.

- Auto-Summon classic/manual entry now uses an explicit visual transaction: stable purchase button, observed menu exit, owned confirmation, reveal/result, and only then the next purchase. It no longer repeats a blind purchase while an unchanged menu remains visible. Co-Pilot's intentional Auto-Summon block is unchanged.
- Purchase targets come from the detected yellow button component. Reveal and result are separate states; the reveal blocks other analyzers, while the settled result can hand control back to the summon transaction.
- Network Defense targets come from visible cyan/purple button components. Matching, the two-button leave question, and the one-button team notice are distinguished by foreground structure. The unchanged start panel no longer counts as a completed battle. Repeated unresponsive Network dialogs stop after three actions and request a diagnostic screenshot.
- Bond rotation accepts only the independently detected green active-partner marker as the original/raised partner. A yellow selection border cannot impersonate the active partner or prove a successful switch. Tall-phone active-marker scoring requires a unique best cell.
- `ColorRegionLocator` supplies normalized connected regions so recognized targets scale with the captured game viewport instead of device pixels.
- Full `testDebugUnitTest` and `assembleRelease` passed. Built APK SHA-256: `f9501b5d8cbca900b45f0af3df09d51c0e405a24b9c7ecac5a6d7fb10b728208`; size 25,610,156 bytes. Certificate SHA-256 matches the expected release identity.
- No device was attached after the build, so installation and physical-device verification remain pending. Do not claim universal handset compatibility until the offline diagnostic runs pass on the real devices.
