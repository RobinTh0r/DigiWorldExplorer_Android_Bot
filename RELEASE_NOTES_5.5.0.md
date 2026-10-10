# DigiWorldExplorer 5.5.0

Regular release packaging the calibration fixes and native BlueStacks testing completed on 9 October. This is not a guarantee that every phone or every automation path is now fixed. Existing experimental Co-Pilot/All-Sprite labels and access rules are unchanged.

## Improvements

- Stable shared image-to-tap coordinates: dark transition seams no longer immediately shrink the game area; real display/window/DPI/capture changes still trigger recalibration. No new handset profiles or hardcoded display resolutions.
- Partner/Bond: observed header, Plus and roster geometry; cyan-framed bubble verification rejects bright battle projectiles and logs collection results.
- Meat Field: observed plot/action/seed/water/timer/close geometry, direct watering-to-field confirmation and independent explicit Farm starts.
- Dungeons: observed page/modal controls, merged card-border/shadow duplicates, safer modal recovery and no Android Back from a falsely detected Home modal.
- DWS: active sessions retain priority over login detection during grid rechecks; measured stock plates and conservative Dash-stock reading. Classic/Bota Walk rules retained; the upper-right broom is not pressed as Dash.
- Co-Pilot overlay: centered rotating arrow, large right status card hidden during the run, compact phase/timer below the badge. The idle visibility preference is retained.

## Actual verification

710 automated tests passed; signed release build and vital lint passed. The existing app ID and signing certificate are retained.

BlueStacks was actually configured and restarted for each native format below, not merely fed cropped/resized screenshots:

| Native resolution / logical DPI | Live result |
| --- | --- |
| 1080×2340 / 420 | 15 Partner switches/bubbles and original restored; Farm/rewards; real green Dashes; exhausted Dungeon survey returned Home |
| 1080×2424 / 420 | 15 switches/bubbles and original restored; Farm/rewards; DWS bounded return after ownership fix; exhausted Dungeon survey returned Home |
| 1220×2712 / 480 | 15 switches/bubbles and original restored; harvest/plant/water and rewards; DWS movement/bounded return; exhausted Dungeon survey returned Home |
| 1440×3168 / 560 | 15 switches and original restored; 14 bubbles confirmed, first previously tapped in an interrupted test; Farm/rewards; real movement/energy/Dashes and independently verified five-minute return |

These are emulator tests, not Samsung/OnePlus/Xiaomi phone certifications. The same game account/model was used. Tests consumed ordinary free game resources, with no paid purchase or daily-ledger reset. The original emulator resolution/settings were restored.

## Still open

- Physical-phone runs, additional display zoom/navigation/tablet variants and a live 1080×2400 run.
- Fresh positive Dungeon battle and ad-attempt flows: the available attempts were already exhausted during this test session. Survey/skip/return-to-Home success does not prove ad execution.
- Some DWS grid-recheck pauses and imperfect exact resource/seed OCR; not every water option was individually proved.

Install as an update, without uninstalling or clearing data. On physical phones, disable USB debugging during gameplay to avoid security-policy error `00000038`.

For a failure, enable in-app Diagnose before the run, stop it afterward and share the ZIP with the device model, resolution/zoom/navigation settings and the failing step. Stop automation before changing display settings, then restart capture. The bot pauses when geometry cannot be confirmed instead of guessing a tap.

## Deutsch

5.5.0 verbessert automatische Kalibrierung, Partnerwechsel/Blasen, Fleischfeld, Dungeon-Erkennung und DWS-Zuständigkeit. Der Co-Pilot zeigt einen mittig drehenden Pfeil und eine kleine Phase statt des großen rechten Statusfelds. 710 Tests und vier tatsächlich umgestellte BlueStacks-Auflösungen wurden geprüft. Echte Handys, frische Dungeon-/Werbeversuche und zusätzliche Zoom-/Navigationsvarianten bleiben offen; gelegentliche DWS-Prüfpausen sind weiterhin möglich. Einstellungen, Daten und gültige Codes bleiben erhalten. Auf Handys USB-Debugging vor dem Spielstart ausschalten und Fehler mit Diagnose-ZIP melden.
