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

Two Discord reports separately describe Dungeon Co-Pilot returning Home early. They supplied no
diagnostic session, so no speculative counter or scheduler behavior was changed. Capture a complete
diagnostic session from starting Dungeon Co-Pilot through the early return before calibrating it.

## Device and build constraints

- OnePlus IN2023 / Android 13 closes the game with security error `00000038` while ADB or USB
  debugging is active. Install first, disconnect and disable debugging, then test with in-app diagnostics.
- Run unit tests and `assembleRelease` with Android Studio JBR and Android SDK/build-tools 36.0.0.
- Verify APK certificate, SHA-256, tag and GitHub pre-release asset before announcing completion.
- Do not claim universal handset compatibility from automated tests alone.
