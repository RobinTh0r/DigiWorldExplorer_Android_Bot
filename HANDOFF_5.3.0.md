# Handoff — 5.3.0 (2026-10-09)

The user explicitly requested the regular **5.3.0 release**, replacing the initial Beta 4 plan. Remote tag `v5.3.0` was absent before publication. This workspace is authoritative; preserve app ID, certificate, preferences and user data. Do not publish another release without explicit authorization.

## Implementation and evidence

- Version code 84/name 5.3.0, ID `de.robinthor.digiworldexplorer`; signing identity unchanged.
- Current changes and evidence are described in `RELEASE_NOTES_5.3.0.md`, `CHANGELOG.md`, `docs/DUNGEON_LIVE_MATRIX_20261009.md` and the historical `HANDOFF_5.3.0_BETA3.md`. Its local/published APK hashes are historical, not the 5.3.0 artifact.
- DWS chooser: free/default Classic / Bota Walk (internal V4_DASH), All-Sprite Test · Beta (internal V5_ALL_SPRITES, existing access gate). V3 hidden, stored legacy settings preserved with a note until the user changes mode. Movement rules intentionally unchanged.
- Green Dash observed automatically, stock 271→264; broom remained 2. HUD splits touching digits, preserves full baseline and uses recorded glyph variants. An unknown suffix with a verified nonzero multi-digit prefix may supply a conservative lower bound (`dashMinimumOnly`), never a guessed exact count. Unknown/zero prefixes, missing stock and broom-only frames remain blocked.
- Reject text-covered reward-strip grid candidates and search actual board edges. Refresh quick status on successful grid overlay updates. No global relaxation of capture/player safety gates.
- Dungeon: global ad-pass gate was off in the baseline; account pass verified by instant manual ticket grant and left ON. Fix cold capture start and fast ad confirmation; retry only same positive Ad action. Bounded Network retained-team inspection, visible counter/ad-button detection, synchronized pass switches. Genuine video ads are not automated.
- Opt-in diagnostics: safe settings whitelist, runtime/daily context, model/Android/display/capture geometry/DPI/insets where supported, and action/transaction evidence. Metadata excludes activation codes/account identifiers/serials; screenshots may contain in-game names. Retention 6 sessions/50 images, context snapshot cap 20.

## Verification / publication

Final 5.3.0 build: **326 tests, 0 failures/errors/skips**, release build and vital lint successful. APK manifest verified ID `de.robinthor.digiworldexplorer`, version code 84/name 5.3.0. Certificate SHA-256 `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8` unchanged. APK `C:/Users/thor/AppData/Local/Temp/dwe-verify-20261003/app/outputs/release/DigiWorldExplorer-Bot-v5.3.0.apk`, SHA-256 `24efa9b5dae7469435885e2f0b3385e2045759032023d8a4abeabd3d70e736a7`. Source/asset publication target: `https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/tag/v5.3.0`, regular release (not prerelease) with explicit limitations in notes. Do not interrupt the user's ongoing BlueStacks DWS run merely to install a UI/version-only upgrade; its local predecessor was installed and tested. This final version/UI-only packaging was not installed on the running emulator. No physical-phone ADB gameplay.

## Remaining work

Publication confirmed: source commit/tag target `62d4d8b`, `main` and annotated `v5.3.0` pushed. GitHub release is public (`draft=false`, `prerelease=false`) and `/releases/latest` resolves to `v5.3.0`. Asset `DigiWorldExplorer-Bot-v5.3.0.apk` is uploaded (25,695,736 bytes); GitHub digest matches the verified local SHA-256 above. Release notes include the test limits. No final-release device installation was performed, preserving the ongoing DWS run. This confirmation is a documentation-only follow-up; code tag remains at `62d4d8b`.

Fresh uninterrupted Dungeon pass with both Network ad tickets, real phones and all aspect ratios/fonts remain unverified. The resource progress detector sometimes reports no Dash progress before animation settles; unchanged, investigate with paired frames if reported. Bond bubbles, other Copilot stages and remaining Metal Sea/Auto Summon reports are not certified by this release. The root latest_detection.txt can retain an older/unknown frame; use current action logs + screenshots, not that file alone. On physical phones keep USB debugging off during the game and use in-app diagnostics.
