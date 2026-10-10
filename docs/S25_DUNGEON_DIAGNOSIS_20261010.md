# S25 Dungeon diagnosis — 10 October 2026

User supplied diagnostic-20261010-170954.zip (v5.5.0/code86). Private original/evidence extracted outside repository at `C:/Users/thor/AppData/Local/Temp/dwe-s25-20261010`; do not upload the ZIP/private baseline configuration.

## Observed phone configuration

Samsung SM-S931B, Android16/SDK36, 1080×2340, density420, fontScale1.15, en-AU. App reports top103/bottom126 insets; capture geometry ultimately full1080×2340, display origin0, scale1.0. The insets are app-window metadata, not proof that those pixels must be cropped from the fullscreen game. Legacy capture, DemiDevimon only, three normal attempts, useAds true but Ad-Skip Pass false (so ads are not permitted). No active VS/Network/Summon/Feed/Bond/Farm.

## Timeline / actual cause

09:10:12Z starts battle at actual743,1670. Failure guide closes at539,1964 at09:10:26,09:10:47,09:11:00Z; paired images show real Dungeon Failed→Attempt panel→next versus screen. Thus those are genuine game defeats, not arbitrary exits during combat. After the fourth start09:11:02Z, returned challenge is treated as WIN at09:11:19Z without reward proof. Waiting clears; a later visible failure guide remains at09:11:39Z and unknown-screen timeout parks the run. The old direct failure branch required waiting="battle", so the late guide was not handled. Current saved images are sampled next-frame/follow-up, not the exact analyzer decision frame; the specific fade frame that authorized the return is not retained.

## Generic changes

- Failure guide handled for the owned active non-Network card regardless of waiting, with once-per-start result accounting; repeated close frames cannot inflate loss totals.
- Returned panel requires stable recognition and does not invent WIN. Reward/result is positive victory evidence; unconfirmed return logged separately.
- Stop retries after consecutive actual losses reach the configured normal-attempt count; verified victories reset that streak. Successful Demi/Bakemon progression retains its existing15 cap. No persistent daily ledger reset.
- Foreground loss veto in panel detector; close positively recognized done panels via right backdrop, not bottom navigation/Android Back.
- Native emulator test additionally reproduced pink announcement falsely matching global Stage Failed. White announcement paper is no longer accepted as medium-gray Growth Guide header. Actual S25 failure and returned-panel images plus announcement negative image are regression fixtures.
- Live Dungeon list9/2 and8/2 were both classified ZERO by the old enclosed-hole shortcut. Replaced it with the existing normalized digit reader; positive and exhausted regression fixtures pass.
- Legacy panel fallback could accept cyan combat effects/cards, then retry Start after combat had already transitioned. Require foreground modal body for non-Network panels and prohibit unaccepted-Start retries after an accepted transition. Two actual battle negatives are fixtures.
- Unconfirmed returns count once per started battle and park at the configured attempt count, without a victory/daily-complete claim. A late observed loss can still resolve the current unconfirmed return; verified win resets streaks.

## Live emulator checkpoint

Native Pie64 framebuffer actually changed720×1280/240→1080×2340/420 while HD-Player stopped, real client restart, wm physical size/density and InputDispatcher logical/physical frames verified. Android font scale set1.15. Android remains9/SDK28, spoof modelSM-S908E, game localeDE; this is NOT an Android16/S25 replica. No artificial S25 manufacturer-specific crop or phone profile introduced.

Windows Computer Use returned black images and failed window activation after retry; scoped native config and emulator ADB used instead. Do not UIA-dump during capture; initial capture-start dump caused a harness accessibility reconnect, not a game fault. ADB instantaneous tap can be missed by Unity; held140–220ms test taps work. Entry/title and new announcements manually advanced; no autonomous-entry certification. Early old/global detector clicked announcement "do not show today" while reproduced; this game popup preference side effect is not a calibrated Dungeon action. Capture stopped for the generic announcement fix.

Live sessions113307,114139,114948 reproduced announcement false-failure, false-empty list and Start retries hitting cards. Guard build session120303: orphan modal closed via right backdrop, positive Demi list card selected, actual battle388 started; multiple returns logged UNCONFIRMED, no mid-battle retry/card taps after the new guard. Final build session120727: starts10:07:52Z,10:08:08Z,10:08:24Z; three unconfirmed returns, PARK10:08:40Z, no fourth Start and no WIN. Capture manually stopped afterward, projection null/0services. No observed WIN/reward or loss guide in this emulator run, so S25 failure recovery is image/state regression-tested, not certified live on S25. No paid purchase; right-hand Challenge left the ticket count9/2 unchanged.

App test settings restored and verified from context17: summon/feed/director card/ad-skip ON; Apocalymon/Factory/Network/Metal/Daily ON, Demi/Bakemon OFF; other saved options unchanged. Diagnose OFF verified from post-boot UI; font scale1.0. Game manually returned to Home. Baseline native configuration restored720×1280/dpi240/custom0/empty custom list with real client restart; wm size/density/font confirmed after boot. Tester UIA reconnect can recreate an inactive "capture off" overlay; final AUTOMATIK STOPPEN performed after the last UIA query, projection null/0services and no app-overlay windows. Client closed to restore its initiallyOFF state. Signed beta installed in place; not uninstalled or rolled back.

Open behavior: Demi/Bakemon always choose the right-hand Challenge. Left-hand "Clear Previous Difficulty" is not detected/executed. DungeonBudgetLedger.shouldClearPrevious exists but no runtime caller and no configured threshold. Do not describe it as an existing working policy or wire a new spend policy without verifying counter/cost/disabled states. That change, successful fresh clears/rewards and ads need separate acceptance coverage. Heavy geometry re-confirmations during dark combat transitions also remain an observation, not a claimed smoothness fix.

Verification:719 complete unit tests, zero failures/errors/skips; signed release build/vital lint successful. Code87, name5.5.1-beta.1; APK25,762,052bytes SHA256bdfb588a72fb99a9b4e5a42adb3e9ea29cf654dfacba3c4126199807510bb46b, original signer859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8. No physical phone tested.

Superseded artifact after user's Bond follow-up: latest full725 tests/build/vital lint pass; APK25,762,504bytes SHA2567ec6901220e05ac03f90eb1b9d003aa63664a1ef7ba9d840c917429c66315257, same signer/code/name. This ZIP contains no Bond session and has Feed/Bond/Copilot disabled, so do not attribute central-partner clicks to this Dungeon evidence. Additional Bond change uses current measured bubble upper-right interior and its viewport; image/synthetic regression tests only, no new live device check. See HANDOFF_5.5.1_BETA1.md for latest state.

Baseline native720×1280/dpi240/custom0/empty custom list/fontScale1.0. Changed test settings to restore: summon/feed ON; director card visible ON; Ad-Skip Pass ON; Apocalymon/Factory/Network/Metal/Daily selected ON, Demi/Bakemon OFF. Diagnose originallyOFF. Other saved options unchanged. Original Partner MetalGarurumon1 untouched. Previous diagnostics backed up before retention. No paid purchases, uninstall or data reset.
