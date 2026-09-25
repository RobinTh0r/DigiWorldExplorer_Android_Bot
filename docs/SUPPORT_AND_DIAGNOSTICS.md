# Support- und Diagnosepaket

Diese Datei definiert die Support-Unterlagen, die zu einem Fehlerbericht gehören. Sie ist zugleich
die Übergabe-Spezifikation für weitere Entwickler oder KI-Agenten.

## Betriebsarten

- **Co-Pilot** (sicherer Standard): Der Nutzer öffnet Farm, Partner, Dungeon oder Gekkomon selbst.
  Der Bot darf nur auf einem eindeutig erkannten Aktivitätsbildschirm arbeiten. Er navigiert nicht
  selbständig durch Home-Menüs.
- **Expedition** (noch in Entwicklung): Der Bot soll eine konfigurierte Aufgabenliste abarbeiten. Navigation beginnt
  ausschließlich auf einem positiv erkannten, dialogfreien Home-Screen. Nach jedem Auftrag muss er
  Home erneut erkennen. Unbekannte Screens parken den Ablauf.

Ein Moduswechsel während einer laufenden Aktion darf erst nach Abbruch der anstehenden Geste wirksam
werden. Neue Module müssen `AutomationModePolicy` verwenden und dürfen die Prüfung nicht lokal
umgehen.

## Nicht sensible Support-Dateien

Der aktuelle Export ist in der App über „Supportbericht exportieren“ verfügbar. Nach einer Vorschau
wird über Androids Dateiauswahl eine ZIP mit `support-report.txt` gespeichert. Sie enthält eine
explizite Auswahl an Gerätedaten, Modus/Feature-Schaltern und maximal 200 strukturierten Ereignissen.
Die ZIP wird nicht automatisch versendet. Die spätere Erweiterung kann folgende Dateien ergänzen:

- `manifest.json`: App-/Android-Version, Hersteller/Modell, Auflösung, Sprache, Zeitzone,
  Automationsmodus und aktivierte Feature-Schalter;
- `session.json`: Task, Phase, letzter erkannter Screen, letzter Frame-Owner, sichere Zähler,
  begrenzte Retry-Zähler und Ergebnis;
- `events.txt`: strukturierter Ringpuffer der letzten Zustandswechsel, Gesten und Timeouts;
- `README.txt`: Hinweise zum Teilen und zur manuellen Entfernung einzelner Angaben.

Standardmäßig **nicht** exportieren: Screenshots, MediaProjection-Frames, Spielernamen, Account-IDs,
Lizenzschlüssel, E-Mail-Adressen, Android-IDs, Tokens oder komplette SharedPreferences. Ein optionaler
Screenshot-Export benötigt eine eigene, gut sichtbare Zustimmung und eine Vorschau.

## Pflichtangaben für einen Fehlerbericht

1. Co-Pilot oder Expedition und betroffene Aufgabe;
2. Startbildschirm und erwarteter nächster Bildschirm;
3. tatsächlich sichtbarer Bildschirm bzw. Dialog;
4. Gerät, Android-Version, Displayauflösung und Spielsprache;
5. ob Anzeigezoom, geteiltes Fenster oder ein Overlay aktiv war;
6. reproduzierbare Schritte und ungefähre Uhrzeit.

## Entwickler-Checkliste

- Supportexport ist explizit, lokal und vor dem Teilen einsehbar.
- Der Ringpuffer hat eine feste Obergrenze und wird bei neuem Capture geleert.
- Koordinaten und Farbstatistiken dürfen protokolliert werden; Rohpixel nur nach Opt-in.
- Fehlerzustände enthalten einen maschinenlesbaren Grundcode.
- Jeder Release-Build nennt Commit/Version, damit Supportdaten reproduzierbar bleiben.
