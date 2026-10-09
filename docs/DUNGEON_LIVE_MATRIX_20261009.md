# Dungeon live investigation — 2026-10-09

Local work after published Beta 3. No new release authorized or published. All times below Europe/Berlin; diagnostic event timestamps are UTC.

## Proven findings

1. At 07:58 the actual settings were `adSkipPass=false`, `dungeon_use_ads=true`, normal limit 3, enabled Apocalymon/Factory/Network/Metal/Daily (Demi/Bake off). Zero-normal-ticket cards skipped by the scheduler and Network's positive ad counter closed because the global pass gate was off. This conclusively explains that baseline, not a zero ad count.
2. Manual Factory ad button immediately granted one ticket without playback: this account owns Ad Skip Pass. The switch was therefore enabled intentionally, not guessed.
3. At 08:12 Factory's fast ad reward was dismissed before the old 1500-ms reward guard accepted it. Retry pressed the now-visible **Challenge** target under an ad transaction; battle ran, but the rotation parked. Fix accepts observed fast ad rewards, confirms a positive returned ticket, and retries only the same positive **ad** button, never Challenge/Matching.
4. First Dungeon start with capture off suffered a service-start race: request could see automation disabled. The local change sets enabled immediately after scheduling service enable. Subsequent cold-start diagnostic confirms capture + rotation on one start.
5. At 08:22 Network ad 1 granted and played, but retained-team/normal-limit handling ended the card while the list still had ad 1/2. Added bounded team-leave/reopen inspection (maximum two, park rather than looping).
6. At 08:32 on 720x1612, ad 2 granted and was confirmed at 1431 ms. Normal ticket recognition then parked. The final detector locates Network's visible cyan counter box and purple ad region, retaining the old Matching-first entry policy. Actual granted-ticket, 1/2 ad and old notice/entry frames are regression fixtures.

## Matrix and limits

| Environment | Geometry / density | Evidence |
| --- | --- | --- |
| BlueStacks Android 9 / SDK 28, spoof Samsung SM-S908E | 720x1280 / 240 dpi | Baseline global-gate failure, manual Factory pass, fast Factory ad retry failure; Network ad 1 grant and battle |
| Same instance, temporary display override | 720x1612 / 240 dpi | New adaptive game UI, Network ad 2 automatic grant; final counter/button screenshot regressions |
| Physical Samsung/OnePlus/Itel | Not tested live | Emulator model spoofing is not hardware/Android/insets/capture/touch equivalence; obtain current full in-app diagnostics with USB debugging off |

Source sessions: `diagnostic-20261009-075616`, `080452`, `081732`, `082827`, `083225`, plus final follow-up. Local backup: `C:/Users/thor/AppData/Local/Temp/dwe-live-investigation-20261009/diagnostics`.

Diagnostics now include opt-in `context-session.json`, settings/start/capture context snapshots (maximum 20), actual image dimensions/strides, model/manufacturer/SDK, display/app dimensions, DPI/font scale/rotation/locale, runtime rules/modules, saved-vs-default safe settings, relevant daily progress, and ad confirmation evidence. Activation strings, serials and account identifiers are not included in metadata; game screenshots can still show in-game names. Screenshot limit remains 50. Android 9 does not expose the added Android 11 window-inset API, recorded as unsupported rather than assumed zero.

Final local build: version 83/name 5.3.0-beta.3 unchanged, 321 tests, no failures; release build/vital lint successful; unchanged signing SHA-256 `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`. Local APK `C:/Users/thor/AppData/Local/Temp/DigiWorldExplorer-Bot-v5.3.0-beta.3-local-diagnostics.apk`, SHA-256 `aafa1bd973d4e17b5be6db4b65e1f9f76569afbef3a83c2f7572111340138883`. Installed with upgrade preserving data. This is NOT the GitHub asset; the reused temporary build-output path has been overwritten, so the old published handoff hash applies only to the official asset.

Open: retained-team bounded recovery needs a fresh full daily cycle to prove both ad attempts in one uninterrupted run. Factory daily ads already exhausted during investigation. Metal Sea, genuine advertisement playback, real phones and other automations are not validated by these results. Do not claim universal compatibility.

Final live follow-up `diagnostic-20261009-083751`: at 08:38:26 the granted Network ticket was recognized on 720x1612 and Matching dispatched; at 08:38:30 the visible Challenge follow-up started stage 415. Reward closed and rotation completed back on Home at 08:39:22. This proves the corrected tall counter plus battle/return, not both ads in a fresh uninterrupted pass. At completion the original 720x1280/240dpi (no override), normal limit 3 and original card selection were restored. Verified Ad-Skip Pass remains ON intentionally. Automation/capture and diagnostic recording were stopped; game left on Home. Sessions were backed up before and after retention rotated old sessions. Changes remain uncommitted; no push/release.
