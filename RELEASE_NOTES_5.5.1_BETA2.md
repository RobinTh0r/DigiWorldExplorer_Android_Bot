# DigiWorldExplorer 5.5.1 Beta 2

Optional testing update based on Samsung S23/S25 reports. Update in place; existing
app ID, signing identity, settings, data and activation codes retained. Stable
5.5.0 remains unchanged. Includes the earlier Beta 1 Dungeon/Bond corrections.

## Changes

- Restored a global Growth Guide check before every automation mode, including
  exclusive Dungeon rotation and Co-Pilot sessions. It no longer requires an already
  selected dungeon. Recognizes the failure title, gray header and guide content.
- Dismissal uses an observed dark backdrop beside the measured guide, not its
  Fight/Skills/Digimon shortcuts or a fixed middle/bottom coordinate. Competing queued
  inputs are blocked; repeated failure frames do not invent additional battle losses.
  Requires fresh confirmation, limits attempts and verifies disappearance separately
  from Android input delivery. Obstructed/unconfirmed closure shows a helpful status.
- DWS stock reading uses a wider measured number area and separates thousands
  commas. Native 1,148/1,139 screenshots now prove a conservative minimum 1,000;
  unknown suffixes are not invented. Missing/zero/uncertain stock still blocks blind Dash.
- Partially rendered startup crops must show continuous persistence before becoming
  a click mapping. Configuration changes invalidate old geometry. Ambiguous grid
  rechecks immediately pause input; one animated mismatch no longer resets everything.
  Old queued bursts cannot restart after a pause or recalibration.
- Expanded opt-in Diagnose: DWS grid/cell scores, HUD readings, wait reasons,
  planned analysis/display clicks, geometry generations and follow-up observations.
  Original-pixel PNG crops preserve stock outlines and sampled tap surroundings for
  Bond/Dungeon and other features. Compact screenshots now retain the recent 50,
  rather than only the beginning of a long run. Bounded logs/native images keep size
  controlled. Export manifest explains which evidence is present or evicted.
- Diagnosis records distinguish input completion from game success; no new
  screenshot writing when Diagnose is disabled. Screenshots may contain personal
  information; review before sharing. Activation codes are excluded from context.
- Corrected existing locale configuration and support-export configuration access.

## Verification and limitations

750 automated tests passed with zero failures/errors/skips. Signed release build,
vital lint and full debug lint passed (full lint: 0 errors; existing warnings remain).
Actual reported native/ZIP images and older failure images were replayed. Negative
fixtures include news, combat/card flashes, Network, Partner, summon and rewards.
Synthetic resolution/border, projection, temporal-state and retention tests passed.

**No new live phone or BlueStacks gameplay test, actual Growth Guide dismissal or
Android ZIP-export end-to-end test was performed for these new changes.** Image
resizing/projection simulations are not device certification. The richer internal
decision trace is DWS-specific; other modes receive the shared gesture/image evidence.
An already queued action rejected during foreground recovery may still park its
caller and require restarting that mode. Not a guarantee for every phone.

Fresh Dungeon wins/rewards/advertisements still need live coverage. "Clear Previous
Difficulty" remains unimplemented; this beta does not make weaker parties win.
On physical phones, keep USB debugging OFF during gameplay (00000038). Enable
Diagnose before starting, reproduce the issue, stop diagnosis and share the ZIP.

## Deutsch

Der Growth Guide wird wieder zentral vor allen Modi geprüft, auch vor exklusiver
Dungeon-Rotation und Co-Pilot. Der Schließpunkt wird neben dem erkannten Fenster
gesucht, nicht auf dessen Karten. DWS liest Tausenderzahlen vorsichtiger, teilweise
gerenderte Startbilder dürfen nicht sofort einen falschen Klickbereich festlegen
und Rasterprüfungen stoppen alte Klickfolgen sicher.

Die Diagnose enthält deutlich mehr Erkennungs-, Klick- und Geometriedaten sowie
scharfe Ausschnitte. Bei langen Läufen bleiben die letzten 50 Screenshots erhalten;
der Export listet tatsächlich enthaltene Belege. 750 Tests und Build/Prüfungen
bestanden. Die neuen Änderungen wurden nicht live auf Handys oder in einem neuen
BlueStacks-Spieltest geprüft; kein Versprechen für alle Geräte. Bestehende Daten,
Einstellungen und gültige Codes bleiben erhalten. Auf Handys USB-Debugging während
des Spiels ausschalten und bei Problemen die In-App-Diagnose verwenden.
