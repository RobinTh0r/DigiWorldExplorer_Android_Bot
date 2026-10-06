# DigiWorldExplorer handoff — 5.3.0 Beta 2

Date: 2026-10-06. This repository and `main` are the authoritative source for `v5.3.0-beta.2`. Read the Beta 1 handoff for prior release history; do not treat older sibling worktrees as the current implementation.

## Release identity

- App ID: `de.robinthor.digiworldexplorer`
- Version code/name/tag: `82` / `5.3.0-beta.2` / `v5.3.0-beta.2`
- Expected signing certificate SHA-256: `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`
- Local release APK: `C:/Users/thor/AppData/Local/Temp/dwe-verify-20261003/app/outputs/release/DigiWorldExplorer-Bot-v5.3.0-beta.2.apk`
- Local release APK SHA-256: `7dd8066cb6a1767d634a717111e665357aa7eb74bbf7e7c21aae642d4053d927`
- Validation: 303 unit tests, zero failures/errors/skips; `assembleRelease`, vital lint, APK signature and manifest verification passed.
- No ADB device was attached during this release preparation, so the new build was not installed or exercised on a physical phone.

## Beta 2 changes

- DWS V3 Classic and V4 Dash are free in the selector and in all runtime reload paths (main app, capture consent, quick overlay). V5 All Sprites + Dash alone requires a Beta code. V4 is the default when no valid profile is saved. An inaccessible saved V5 choice falls back to V4 at runtime without erasing the saved preference; activating a code restores that choice.
- The uncommitted Beta 1 follow-up is included: more visual Partner panel/roster geometry, Plus and confirmation target detection, bounded accidental detail-popup recovery, session-wide Bond and Network ownership, and World Search title-guard.
- Copilot Home rewards use visible chest/dialog/button regions instead of German text at fixed 720-pixel positions. Reward ownership, bounded retries, deadline and opt-in diagnostic events/screenshots were added; the Copilot run completes after rewards when no DWS excursion was selected.
- The large right status card is hidden during Dungeon Rotation. A small animated click-through badge under the floating icon indicates active Copilot outside Dungeon Rotation.

## Known unverified reports

Do **not** claim all-device compatibility. Reports supplied at release time include early Dungeon return/Home and game-exit prompts; missed Network Ops free/ad attempts and Metal Sea normal/ad attempts; Bond stalls or failure to expand on Samsung S24/S25 and BlueStacks; DWS dash/loop/square movement on phones; and Auto Summon not acting for some users. Those are not all fixed by this Beta. The quoted Discord messages are reports, not diagnostic evidence. A 328-byte diagnostic ZIP contains no useful game frames. Obtain a full in-app diagnostic session spanning the failure and calibrate against the affected phone/resolution before changing those routes.

## Safety and release constraints

- Preserve the app ID, certificate, settings and user data. Signing files are ignored; never commit or expose keystore material.
- OnePlus IN2023 / Android 13 has previously closed the game with security error `00000038` under ADB. Disconnect and disable USB debugging during gameplay, then use in-app Experimental diagnostics.
- Check `git status` and device identity before editing, installing or tapping. Do not publish another release without explicit user request.
- The release body is in `RELEASE_NOTES_5.3.0_BETA2.md`; verify GitHub tag and asset hash after publishing.
