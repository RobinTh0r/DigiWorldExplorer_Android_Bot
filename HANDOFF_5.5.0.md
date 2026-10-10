# Handoff — 5.5.0 (10 October 2026)

Authoritative checkout: this folder. The user explicitly requested publication, then corrected the proposed Beta to a regular full release ("5-5 oder so"). Current identity: versionName `5.5.0`, versionCode `86`, tag `v5.5.0`. Release notes: `RELEASE_NOTES_5.5.0.md`. Experimental feature labels/access rules remain unchanged; regular release is not all-device certification.

## Implementation

Packages the live-tested local work documented in `docs/BLUESTACKS_LIVE_CALIBRATION_20261009.md` and historical checkpoints in `HANDOFF_5.4.1_BETA1.md`. Shared capture/game/display projection, canonical analyzer viewport and temporal content stability (three continuous seconds before shrink; tiny seams ignored; real metadata changes invalidate immediately). Observed Partner header/Plus and moving framed bubbles; measured Farm plot/action/seed/water/timer/close controls and direct watering-to-field verification; independent explicit Farm starts. Dungeon measured header/modal geometry, duplicate contour merging, positive Home veto and backdrop recovery instead of Android Back. Active DWS sessions exclude generic Entry during grid rechecks, measured stock plates/conservative stock reading and cell-relative bounds tolerance. No new handset profiles or resolution-specific branches.

Requested overlay uses a centered vector arrow, compact phase/timer and temporary right-card hiding during Copilot, preserving idle preference. English/German in-app changes updated. Classic/Bota Walk free and All-Sprite Beta rules retained.

## Actual verification and limits

9 October: 710 automated tests passed, release/vital lint passed. Real BlueStacks native framebuffer changes and client restarts, physical size/density verified without overrides:

- 1220×2712/480: full15 switches/all15 bubbles/original1 restored, actual harvest3/plant5/water7, rewards, DWS movement and bounded return, safe exhausted Dungeon survey→Home.
- 1080×2340/420: automatic title entry, full15/all15 bubbles/restored1, Farm/rewards, actual green Dashes, safe exhausted Dungeon survey→Home.
- 1080×2424/420: full15/all15 bubbles/restored1, Farm/rewards, safe survey→Home; DWS recheck ownership fix then actual bounded return from correct Home restart. Earlier wrong-page tester restart is not autonomous pass.
- 1440×3168/560: actual WATER→FIELD follow-up, rewards, DWS energy/movement/Dashes/natural five-minute return. Final overlay build15 switches/restored1 with14 bubbles (first previously tapped in interrupted run), Farm/rewards→DWS motion; last run manually stopped.

No physical-phone or fresh positive Dungeon battle/ad-attempt validation. Attempts were exhausted; daily ledger not reset and no paid purchase. Live1080×2400, additional zoom/navigation/tablet variants remain open. Some DWS PAUSE/grid rechecks persist; exact seed/water/stock OCR imperfect. Do not claim universal perfection or every water/ad option verified.

## Device/data state

End of 9 October: original native720×1280/240/custom0/empty list restored with actual restart and wm resets after boot. Baseline captured preferences exactly restored (summon/feed/Bond/Farm ON, VS/network OFF, rewards/DWS OFF, Diagnose OFF), original MetalGarurumon1, automation/capture OFF, projection null/0services. Free game resources consumed by tests cannot be restored through preferences. Evidence/private backups in `C:/Users/thor/AppData/Local/Temp/dwe-live-20261009`; do not upload private baseline config.

10 October publication check: no connected ADB device; no emulator launched, settings changed, game tapped or APK installed. Retain ID `de.robinthor.digiworldexplorer`, certificate SHA256 `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`; update without uninstall/data clearing. Physical-phone gameplay requires USB debugging OFF and in-app Diagnose. Never UIA-dump during active capture (suppresses/reconnects accessibility).

## Publication

Pre-publication verification completed on version86: **710 tests in81 suites, zero failures/errors/skips**, release build/vital lint successful. Manifest ID/code/name verified and certificate unchanged. APK `C:/Users/thor/AppData/Local/Temp/dwe-release-20261010/build/app/outputs/release/DigiWorldExplorer-Bot-v5.5.0.apk`, 25,762,536 bytes, SHA256 `d43cf0c98e9818ee83f5c119a5930b098a257fd3b424b55eebd4dd5686565054`. Build outputs redirected outside Nextcloud through release-build.init.gradle; tasks `:app:testDebugUnitTest :app:assembleRelease`. Prior remote main `106be06062f7faec744f3227ecf25f3148c839e7`; tag v5.5.0 absent before publication. Upload pending; preserve previous Beta tag/assets. Record final source commit, GitHub asset digest and regular-release flags here after publication; documentation-only confirmation can follow without moving the verified code tag.

## Next work

Collect real-phone in-app diagnosis at failing step, examine geometry generations/current mapping/action outcomes; test actual display/zoom/navigation changes between capture starts. Test fresh positive Dungeon battles/ads without resetting account ledger or purchasing attempts. Continue DWS grid-pause and exact resource-reader investigation with paired screenshots. Avoid device-specific profiles. No subsequent release without new explicit user request.
