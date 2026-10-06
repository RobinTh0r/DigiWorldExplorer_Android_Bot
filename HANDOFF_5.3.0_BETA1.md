# DigiWorldExplorer handoff — 5.3.0 Beta 1

Date: 2026-10-03. This repository and `main` are the authoritative source for `v5.3.0-beta.1`.

## Release identity

- App ID: `de.robinthor.digiworldexplorer`
- Version code: 81
- Version name/tag: `5.3.0-beta.1` / `v5.3.0-beta.1`
- Expected signing certificate SHA-256: `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`
- Release APK: `app/build/outputs/release/DigiWorldExplorer-Bot-v5.3.0-beta.1.apk`
- Release APK SHA-256: `a034778c106238484dc9a14620055478a86b9d0de282f91355b5c07bb0731c34`
- Signing files are ignored. Never commit or expose the keystore or `keystore.properties`.

## Beta 1 change

Samsung SM-S948U diagnostics proved that the generic title/login probe alternated with an active
Network Defense session and tapped the centre skill card at `(540, 1632)`. A confirmed Network
Defense session now excludes generic entry handling throughout battle, loading and obscured frames.
`FrameProbePolicyTest` locks this ownership rule.

## Unreleased follow-up

Further unreleased Copilot/Home reward work: the Home chest, idle reward modal, claim/ad buttons
and result overlay now use visual regions scaled to the observed game viewport instead of German
text at fixed 720-pixel positions. The reward session owns transitional frames; a visually
unchanged claim/close may retry twice. Opt-in diagnostics record chest, dialog and action decisions,
and a run without DWS now completes when the Home reward check finishes. Dungeon Rotation hides
the large status card; an active Copilot shows a small animated click-through badge beneath the
floating icon. Fixture tests cover 360/720/1080 widths and vertically shifted captures. The
reward claim has a bounded deadline and stops Copilot with a diagnostic record if unconfirmed.
These changes are not yet validated in
a live game on a physical phone; do not claim universal device compatibility.
Local verification after this follow-up: 302 unit tests, zero failures/errors/skips;
`assembleRelease` and signature verification succeeded. The unreleased APK at the same temp path
now has SHA-256 `ffd093f995588defe214c41c61152d200099e78d80a694e1269c385b9b7ec6fe`.
No ADB device was attached on 2026-10-04, so no app install or live run was performed.

Read `docs/AUTOMATION_STRUCTURE_AUDIT_20261003.md` for the CPH2611 evidence, dynamic Partner
geometry, expanded-grid transition fix and session-wide Bond/Network ownership added after Beta 1.
Read `docs/DIGIAUTOTAP_1_3_REVIEW_AND_DEVICE_PLAN_20261004.md` for the latest release comparison,
new Partner-detail popup recovery, World-Search title-guard, and the remaining device test sequence.
The new diagnostic proves an already open Partner grid was rejected by fixed hero-border probes.
Existing fixed fallbacks elsewhere still exist; do not describe all modules as fully dynamic.
Expand now requires an observed Plus glyph; Confirm targets the observed aligned dialog-button pair.
Their fixed tap coordinates were removed. Partner-tab/Home navigation remains unchanged pending
real Buddy/Support fixtures. Expansion retries require fresh Plus evidence and remain bounded.
Local verification on 2026-10-04: 297 unit tests, zero failures/errors/skips; assembleRelease succeeded,
and the signing certificate matches the release identity. The unreleased APK is under
`C:/Users/thor/AppData/Local/Temp/dwe-verify-20261003/app/outputs/release/` with SHA-256
`3cfcde400966625e92d5c908979acf726ac9e838358f0eb6d114afd31a49be2a`.
Version remains Beta 1; this local APK is not the published Beta 1 asset. No device was connected,
and no live-game compatibility claim, push or release was made.
The small 160618 ZIP contains no game frames, and the all-session ZIP duplicates the supplied sessions.

Two Discord reports separately describe Dungeon Co-Pilot returning Home early. They supplied no
diagnostic session, so no speculative counter or scheduler behavior was changed. Capture a complete
diagnostic session from starting Dungeon Co-Pilot through the early return before calibrating it.

## Device and build constraints

- OnePlus IN2023 / Android 13 closes the game with security error `00000038` while ADB or USB
  debugging is active. Install first, disconnect and disable debugging, then test with in-app diagnostics.
- Run unit tests and `assembleRelease` with Android Studio JBR and Android SDK/build-tools 36.0.0.
- Verify APK certificate, SHA-256, tag and GitHub pre-release asset before announcing completion.
- Do not claim universal handset compatibility from automated tests alone.
