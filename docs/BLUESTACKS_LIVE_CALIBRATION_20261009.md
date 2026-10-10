# BlueStacks live calibration — 9 October 2026

Completed 9 October test record, NOT universal phone compatibility certification. The historical development builds below used local code85/name5.4.1-beta.1; the published Beta artifact was not replaced. On 10 October the user authorized packaging these fixes as the regular 5.5.0 release; publication is complete with710 fresh version86 tests and matching signed APK/GitHub asset digest. Read `HANDOFF_5.5.0.md` for publication verification; dated local-build and restoration statements below describe the earlier test session.

## Native live matrix

After the user enabled accessibility, the published Beta failed Copilot at all four formats. Each native Pie64 framebuffer/density was changed while HD-Player was stopped and verified after a real client restart, with Android wm overrides reset. Not cropped screenshot simulation, not physical-phone validation. Logical DPI is a test density, not physical PPI or an OEM-default assertion. Spoof model SM-S908E unchanged.

| Native pixels / DPI | Published Beta actual result | Local fix live retest |
| --- | --- | --- |
| 1080×2340 / 420 | Home→Partner works; Partner not recognized, parks | Automatic title entry, Plus, all15 switches/bubbles/restoration, Farm, rewards, actual DWS Dash179→177, safe zero-attempt Dungeon survey→Home verified |
| 1080×2424 / 420 | Collapsed roster falsely15 cells/selected11 instead of1 | Actual restart; safe Dungeon survey, all15 switches/bubbles/original restored, Farm/rewards, DWS entry and bounded no-progress return after ownership fix |
| 1440×3168 / 560 | Partner falsely Unknown/Home, parks | Actual restart; safe Dungeon survey, water field follow-up, rewards, real DWS movement/Dashes/energy collection and five-minute return. Latest UI build15 switches/original restored,14 bubbles; first already tapped in interrupted preinstall run. Farm/rewards/DWS follow-up passed; last DWS manually stopped after motion/UI check |
| 1220×2712 / 480 | Collapsed roster falsely expanded | All15 switches/bubbles/original restored; actual harvest3/plant5/water7, growing Farm/rewards/DWS movement/natural bounded return; safe exhausted Dungeon survey |

Representative formats, not a sales ranking. User redirected work: finish1220 first, then retest other3. Standard DPI first; zoom/navigation variants not live-tested yet. Sources: [GalaxyS24](https://www.samsung.com/ca/business/smartphones/galaxy-s/galaxy-s24-marble-gray-256gb-sm-s921wzaexac/), [Pixel9](https://store.google.com/magazine/compare_pixel?hl=en-US&toggler0=Pixel+9), [OnePlus13](https://www.oneplus.com/global/13/specs/), [Xiaomi14T](https://www.mi.com/pl/product/xiaomi-14t/specs/).

## Reproduced defects / generic fixes

- Partner header measured in broad game-relative region; visible Plus vetoes false expanded roster; Partner sheet vetoes background Home controls.
- Temporal conservative content-area union prevents black animation seams causing repeated geometry generations; ≤2pixel seam tolerance, canonical analysis viewport. Real configuration/density changes still invalidate geometry.
- Projectile incorrectly accepted as Bond bubble: paired actual JPEGs reproduced it. New cyan arc/four-sided frame/interior/icon proof passes regression and complete15 live tours at1220/1080×2340. Actual moving bubbles can require another measured tap; no blanket center-figure tapping.
- Farm observes soil rows/columns, actual shovel/yield/water bubbles and lock. Resource reading follows measured slots; modal action requires wide button, not HUD watering icon. Growing proof uses actual timer plate instead of mandatory perfect8digit legacy OCR. Far-right close square observed instead of oldx=.835.
- Explicit Farm start now independent of Bond cooldown; disabled Bond clock no longer instantly aborts optional DWS. DWS card/close target measured.
- UPDATED DWS HUD measures four visible stock plates rather than truncating digits at fixed raster offsets. Unknown glyphs remain unknown; actual179 may conservatively prove minimum100. Legacy path/broom/zero safety retained.
- Loading blue splash no longer classified Explore from blue alone. Updated title requires independent game-logo evidence; Network battle veto protects all title fallback paths. Native title entry passed1080×2340/2424.
- Dungeon title/action/header measured at actual positions. Positive Home veto and foreground modal-body evidence prevent introduced Home-as-orphan regression; no Android Back recovery. Actual list cyan border/shadow contours with >65% bounding overlap merge into one card before assigning ordered keys; native2424 reproduced Network-as-MetalSea and test protects against this.
- Native2424 transitions can hold a black top seam21px for over750ms: content shrink now needs3continuous seconds; display/window/density/capture changes still invalidate immediately. Expansion immediate, tiny seams ignored, metadata not persisted. Recorded transition regression plus live stable-generation3 survey passed.
- Farm watering can return directly to FIELD without a second WATER dialog. Controller accepts only positively observed RIPE or GROWING-with-water-gone target, not unknown/unchanged field. Software regression passes; native1440 later passed actual WATER→FIELD follow-up without old30sec stall. Water-choice resource OCR remains imperfect; no claim that every watering option/ad path is independently proved.

## Local live1220 evidence

| Session (local time) | Actual result / remaining limit |
| --- | --- |
| 19:07:19 | Eight Partner switches; tester's open quick panel hid RAISE and caused timeout. Harness interruption. |
| 19:15:39 | Geometry stable generation3, all15 switches, original MetalGarurumon restored. Bubble collection not universally successful: projectile false positive. |
| 19:35:39 | Harvested3 ripe, planted all5 unlocked with best free seed9→4; exposed growing/water follow-up defects. |
| 19:42:04 | Actual water bubble/modal clicks, consumed7 small cans; growing timer then stalled. |
| 19:48:40 | Growing field→Explore→Home→reward chest→claim→result→empty→complete→DWS passed. Exposed instant Bond-clock return/old close point. |
| 19:51:58 | Explicit Farm with BondOFF stalled on Home; reproduced/fixed. |
| 19:58:05 | Fixed Farm→Home→empty reward→DWS. Actual movement41,875→41,882m, later resumed run continued to41,953m. Natural5minute return passed18:06:19Z→Home18:06:20Z. Cell-relative grid tolerance accepts13px settling but rejects40px translation/100px resize. |
| 20:07:31 | All15 switches and all15 bubble-collected outcomes, original MetalGarurumon restored, growing Farm→reward claim/result/empty→DWS. DWS later bounded return with steps0; not wrong geometry proof. |
| 20:20:26 | First generic modal-title fix introduced false Home-as-orphan; Back opened quit-to-title prompt. Stopped/cancelled, no logout. Fixed positive Home/foreground modal veto, safe outside-backdrop recovery. |
| 20:24:08 | Fixed full zero-attempt Factory→Network(Leave team)→MetalSea survey→Home, no quit prompt. No actual remaining ad/battle attempt available. |

## Local live1080 follow-up

Session203537/native2340: auto title→entry rewards→Home, then all15 switches/bubbles→original restored→Farm→Home rewards→DWS. Actual water Ad-Skip returned FIELD/RIPE but old controller waited30sec for extra dialog, recovered and harvested/replanted best seed4→3. Source correction above not yet fresh-water live verified. DWS two automatic green Dashes179→177, distance41953→41960. Early bounded return at steps depleted; no painter/broom button pressed.

Session205614/native2340: fixed actual header gate, safe zero-attempt full Dungeon survey→Home18:56:57Z. Subsequent growing Farm→claim/result/empty rewards→DWS→Home18:57:48Z. No water bubble remaining.

Native2424 actual client restart: session205841 reproduced shrink-to-top21px generation3→4→5→6 while leaving Network; stale action parked despite actual Leave succeeding. Session210406 with longer shrink proof retainedgeneration3 but exposed duplicate cyan border/shadow shifting MetalSea onto Network. Exact native screenshot added. Session210839 with contour merge: Dungeon→Home COMPLETE19:09:19Z; all15 switches/bubbles19:10:10→19:13:16Z; original restored→Farm→rewards→DWS. Clean-grid recheck then lost calibration and stale generic Entry monopolized frames. Fixed session ownership excludes Entry throughout an active DWS excursion. Session211707 correct Home restart→Farm/rewards/DWS19:20:48Z→safe no-progress return Home19:21:37Z. Earlier211707 restarted from wrong page by tester, not autonomous pass. Actual2424 image regression proves correct Botamon cell(3,1), not energy below; later unmatched-time log must not be called player-confusion proof.

## Local live1440 and requested overlay

Session220956/native1440×3168/560: automatic title/entry→Home, safe exhausted Dungeon survey→Home20:13:08Z. Farm WATER→FIELD follow-up/growing→Home→rewards→DWS20:14:11Z; actual normal moves, orange energy pickups and green Dashes. Early distance41961→41992, genuine energy replenished steps to3548, dash177→171. Automatic five-minute return→Home20:19:12Z. Still intermittent PAUSE/grid rechecks and large top-bound corrections; not a flawless smooth-speed certification.

Timer auto-started Bond while preparing UI installation; tester stopped after first partner2 bubble tap. Restored original MetalGarurumon1 manually, collapsed roster and returnedHome. New overlay build session222402: observed Plus, all15 switches,14 bubble-collected outcomes; first bubble-not-found consistent with already tapped partner2 just minutes earlier. Original1 restored, Farm/growing→rewards→DWS20:29:37Z; actual movement to42015m, steps3522/claws85/dash166/broom2 at screenshot. This last DWS was manually stopped after motion/UI check, not a second natural five-minute return proof.

User-requested Co-Pilot UI: right director card temporarily hidden during Co-Pilot (Dungeon already hidden), idle preference preserved. Non-font vector arrow/square ImageView pivots at measured center; 92×30dp pass-through badge with compact phase/timer below. Live successive Bond/DWS screenshots show hidden right card, unchanged badge position, different arrow angles and correct phase label. Two pure visibility tests protect idle preference. Animation isn't certified from a single still.

An early Home screenshot was mistakenly suspected as false grid calibration. Chronological paired images prove actual DWS opened; NOT a reproduced false-Home-grid defect. Intentional capture shutdown can discard an inaccessible buffer; distinguish from active-run crash.

Latest full verification:710 automated tests, zero failures/errors/skips; release build/vital lint passed. Latest built/installed SHA256 `7b43dc429c9c398f2c0d24c8bb8d3e26cd8d3b2814dfc85a36bdb8afcbabc24d`, signing SHA256 `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`. Same local code85/name5.4.1-beta.1, not published. These image/software tests do not prove phone compatibility.

## Harness, resources and verified restoration

Never uiautomator-dump during capture: it suppresses/reconnects accessibility, closes quick menu, resets geometry. Expanded quick menu occludes evidence while timers continue; do not leave it over a tour. Use screenshots/events. No physical-phone ADB gameplay: USB-debugging causes game00000038, use in-app Diagnose.

Windows Computer Use repeatedly failed activation/corrupted GPU screenshot; no alternative Windows UI automation used. Native config/API and emulator ADB used. Android-only reboot went offline; full scoped HD-Player restart recovered. ADB aliases127.0.0.1:5555/emulator-5554 are same VM.

Local evidence/backups: `C:/Users/thor/AppData/Local/Temp/dwe-live-20261009`; private baseline config must not be uploaded. Prior diagnostics backed up before6session retention. Normal free game resources consumed: best seeds9→4 verified, another planting later with4 observed before selection (3 inferred, not final measured stock); small cans7→0, meat74495→76903 plus later harvest. DWS steps100→0 then real energy replenishment to3548; later screenshot3522/claws85/dash166/broom2 (loot causes stocks to fluctuate). Water Ad-Skip used with existing pass at earlier native2340. No paid purchase, uninstall or data reset. Consumed game resources cannot be restored by preference rollback.

RESTORED native720×1280/dpi240/custom-selected0, empty custom list, real client restart, wm size/density reset AFTER boot; physical size/density verified. Original MetalGarurumon1 restored by last tour before stopped tests. All captured settings in baseline-current-settings.json match restored-settings.json (exact comparison; no differing keys). Summon/feed/Bond/Farm ON, VS/network OFF, Copilot rewards/DWS OFF, DiagnoseOFF. Automation false, projection null/0active services; stopped at game title after final baseline restart, MainActivity idle. Existing ID/cert/data retained; local development APK remains installed, no publish/commit/push. No physical-phone, fresh Dungeon battle/ad-attempt, live1080×2400 or extra zoom/nav/landscape certification. Four representative native formats above only.
