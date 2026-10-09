# Handoff — 5.4.1 Beta 1 (2026-10-09)

The user explicitly requested publication of **5.4.1 Beta** for their own testing, superseding the unexecuted 5.3.12 naming request. Release identity: code 85, name/tag `5.4.1-beta.1` / `v5.4.1-beta.1`, GitHub prerelease. Preserve app ID `de.robinthor.digiworldexplorer`, existing certificate, preferences and user data. No device installation is part of this publication request.

## Implementation / evidence

Read `docs/AUTO_CALIBRATION_AUDIT_20261009.md` for the complete implementation, actual software test matrix and limitations. Release body: `RELEASE_NOTES_5.4.1_BETA1.md`; changes: `CHANGELOG.md`. This publication packages the calibration work already discussed with the user; no new phone profiles or feature movement/access-rule changes.

Shared display/window/projection geometry now feeds game-local `AnalysisImage` recognition and mapped physical taps/swipes/grid drawing. Geometry generations revoke old delayed actions across configuration changes/gaps; capture resizing, observed borders, periodic clean-grid verification, small Plus recognition and bounded diagnostics were added. App identity and signing certificate must remain unchanged.

**No live emulator or phone gameplay validation of this new calibration code has been performed.** Do not treat the historical 5.3.0 live Dash/Dungeon tests as proof of this Beta. Automated recorded-image resizes do not simulate game/font reflow or real Android callbacks. Physical phones must run without USB debugging and use in-app Diagnose.

## Publication verification

Publication is being prepared at the user's explicit request. Re-run 672 tests/build/vital lint on version 85; verify APK manifest/signature/hash, then push source/tag and create a GitHub prerelease with the verified APK. Append confirmed publication details once GitHub and uploaded asset digest are verified. Do not mark this Beta latest stable or overwrite v5.3.0.

Pre-publication verification completed on version 85: **672 tests, zero failures/errors/skips**, `assembleRelease` and vital lint successful. Manifest ID/name/code verified; certificate SHA-256 `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8` unchanged. APK `C:/Users/thor/AppData/Local/Temp/dwe-verify-20261003/app/outputs/release/DigiWorldExplorer-Bot-v5.4.1-beta.1.apk`, 25,729,016 bytes, SHA-256 `d44490bf4aedadd600fd75f5338a565f249adb4c0d83f71a5d9ca3fbdd49a9ab`. Previous remote main was `3f6f507`; requested tag did not exist before this release. No device settings, packages or game run changed.

## Next work

User device testing with Diagnose, including real resolution/zoom/navigation changes between capture starts. Inspect `geometry-current.json`, mapped gesture events, rejection reasons and paired images for failed Plus/roster/bubble/seed/ad/Matching steps. Keep calibration device-independent. No additional release without explicit user authorization.
