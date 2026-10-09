# DWS nach dem Spiel-UI-Update (offen)

Stand: 2026-10-08. BlueStacks/Spiel 1.5.0, 720×1280, erster echter Frame erfasst; physische Geräte weiter offen.

## Ziel

- **V4 Dash:** Die Bewegungs- und Sammelentscheidungen aus Tag `v4.0.0` bleiben erhalten. Nur die gemeinsame visuelle Eingabeschicht (Raster, HUD, Test-/Dash-Knopf, Szenenwechsel) wird an das neue Spiel-UI angepasst. Kein fester Pixelpunkt pro Telefon.
- **V5 Alle Sprites:** Eigenständiger Beta-Pfad mit generischer Sprite-Erkennung und eigenen Sicherheitsregeln. Erkennt dieselbe neue Oberfläche, ohne V4-Entscheidungen zu verändern.
- **V3 Classic:** Unverändert, außer gemeinsam notwendiger UI-Erkennung.

## Vorgehen

1. Einen vollständigen In-App-Diagnoselauf auf dem aktualisierten Spiel erfassen: Startbild, DWS-Raster, HUD mit 0 und >0 Dash, Test-/Dash-Knopf, Bewegung, Belohnung, Rückkehr. Auflösung und ausgewähltes Preset notieren. ADB während des Spielens auslassen (`00000038`).
2. Alte und neue Bildschirmbereiche anhand der Screenshots vergleichen. Für Raster, HUD-Zahlen und Knopf getrennte Bild-Erkennungsregeln und Konfidenzen festlegen. Bei unbekanntem Knopf keinen Koordinaten-Fallback als sichere Erkennung ausgeben.
3. Die UI-Erkennung in eine gemeinsame, profilunabhängige Beobachtung kapseln; V4- und V5-Planung separat halten. Diagnose soll erkannte Bounds, Knopf-Kandidat, Zähler, Profil und Ablehnungsgrund pro relevanter Szene protokollieren.
4. Auf echte neue Frames Regressionstests für mindestens zwei Seitenverhältnisse und die Übergänge DWS/Menü/Belohnung anlegen. V4 zusätzlich gegen eingefrorene Entscheidungen aus `v4.0.0` prüfen; V5 mit allen Sprites separat testen.
5. Danach eine nicht-technische Preset-Ansicht entwerfen: drei klare Optionen, V4 als Standard, V5 deutlich „Beta“, kurze Wirkung statt vieler Zahnräder. Mehrfachzugänge auf der Hauptseite zusammenführen; keine Änderung der Lizenz-/Einstellungsdaten.

## Befund und Zwischenstand

- Neuer BlueStacks-Frame unter `app/src/test/resources/dws_1_5_bluestacks.png`: 5×5-Raster ungefähr `(80,346)..(623,801)`, Botamon `(4,0)`, linker grüner Dash-Button etwa `(428,1125)`, Dash-Vorrat `271` in der dritten Ressourcenzeile. Der obere rechte Pinsel/Schaufel-Knopf mit Vorrat `2` ist **kein Dash**.
- Die alte Dash-Farbsuche lieferte `(489,1080)` zwischen den zwei blauen Aktionsknöpfen. Der blinde relative Ersatzpunkt war ebenfalls nicht belastbar. Beides wird durch eine Form-/Komponentenprüfung für den grünen Dash-Knopf ersetzt; der Pinsel darf nicht als Dash-Ziel geliefert werden.
- Neuer Regressionstest prüft Raster, Botamon, Knopf und Vorrat für V4/V5 auf diesem Frame. Ein vollständiger automatischer Lauf und echte Handy-Auflösungen sind noch nicht bestätigt.
- Kurzer lokaler BlueStacks-Lauf der nicht veröffentlichten Fassung: Raster und anfängliche Bewegung funktionierten; danach erschien ein Spiel-Dialog und die App pausierte bei verdecktem Raster. Der Bot wurde gestoppt. Das ist kein Nachweis für einen vollständigen DWS-Durchlauf.
- Ein unbekannter Dash-Zähler ist auch unter V4 **kein** Grund mehr für den historischen Vorrats-Fallback `3`, auch nicht auf dem alten HUD. Der alte S22-Frame zeigt sichtbar 0, wird aber noch nicht sicher gelesen. Ein erkannter grüner Knopf und ein positiv erkannter Vorrat sind für Dash nötig; der neue Vorrat 271 bleibt derzeit unbekannt und sperrt Dash.

## Freigabekriterien

- Kein Dash/Test-Tap ohne nachgewiesenen Knopf und sicheren DWS-Kontext.
- V4-Entscheidungen entsprechen den alten Referenzfällen; neue UI-Positionen werden dynamisch erkannt.
- V5 kann separat geändert und zurückgesetzt werden, ohne V4 zu beeinflussen.
- Tests und Build erfolgreich; mindestens ein echter Telefonlauf mit Diagnose. Bis dahin keine Behauptung „funktioniert auf allen Handys“.
