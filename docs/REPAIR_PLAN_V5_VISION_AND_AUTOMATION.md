# Reparaturauftrag: v5 Auto-Summon, Bond und Dungeon auf echten Handys

Stand: 2026-09-29. Arbeitsauftrag für SOL oder Qwen. Dieses Dokument ist ein Plan, keine Bestätigung funktionierender Reparaturen.

Verbindliche Nutzerpräzisierung: Der Auto-Summon-Fehler betrifft den klassischen manuellen Einstieg. Der Nutzer öffnet das Beschwörungsmenü selbst und erwartet dort funktionierendes Auto-Summon. Die bestehende Sperre von Auto-Summon während Co-Pilot ist gewollt und bleibt unverändert. Keine automatische Navigation zum Summon-Menü ergänzen. Das etwas hakelige Verhalten beim Zusammenspiel ist für diesen Summon-Auftrag kein Änderungsziel.

## 1. Auftrag und Grenzen

Repariere die Bildschirm-Erkennung und die darauf aufbauenden Abläufe für Auto-Summon, Bond/Digi Co-Pilot und Dungeon Co-Pilot. Nutze die vorhandenen Diagnosepakete und Referenzbilder. Benutzerbeobachtungen sind maßgeblich: Die bisherigen lokalen Beta-2-Korrekturen führten zu falscher Partnerwahl, Stillständen und falschen Network-Defense-Klicks.

Arbeite in diesem Repository. Lies zuerst AGENTS.md sowie HANDOFF_5.0.3_BETA1.md und HANDOFF_5.0.3_BETA2.md. Die Aussage „release-ready“ im zweiten Handoff ist durch die späteren Fehlversuche überholt. Sichere den Ausgangszustand durch git status, git diff und eine gezielte Dokumentation der bestehenden Änderungen. Kein Reset, keine pauschale Rücknahme, keine Änderungen in alten Geschwister-Worktrees. Vor Geräteaktionen das Zielgerät prüfen. Keine ADB-Spieltests: Auf dem OnePlus löst das Sicherheitsfehler 00000038 aus. Offline-Auswertung ist ohne ADB möglich.

App-ID, Signatur, Einstellungen und Daten erhalten. Keine Veröffentlichung, kein Git-Push und keine Anschaffung externer Dienste aus diesem Plan ableiten. Installation nur im bereits vereinbarten Geräteworkflow; danach ADB beenden. Kein Cloud-Bildmodell für jeden Frame. Opt-in für Auto-Summon und Ad-Skip respektieren; keine neuen Ausgaben freigeben.

Bearbeite die Schritte in Reihenfolge. Pro Schritt: Problem nachstellen, kleinste zusammenhängende Korrektur implementieren, Abnahmekriterien prüfen, Ergebnis dokumentieren. Nicht mehrere Detektoren gleichzeitig anhand vermuteter Koordinaten verändern. Falls Belege fehlen, den konkreten fehlenden Zustand benennen und unabhängig lösbare Schritte fortführen.

## 2. Gesicherte Befunde und offene Hypothesen

### Auto-Summon

Relevante Dateien unter app/src/main/java/de/robinthor/digiworldexplorer/:

- capture/ScreenCaptureService.kt: SummonRewardFrameGuard läuft vor der normalen Modulreihenfolge. RewardPurchaseFrameAnalyzer wird später nur mit featureFrame und !digiCopilotOwns aufgerufen.
- purchase/SummonRewardFrameGuard.kt: Ergebnis-Erkennung reserviert den Frame; nach letztem Treffer gilt eine Sperrzeit von 8000 ms. Der Status fordert manuelles Schließen. Während der Sperre wird onRewardScreen aufgerufen.
- purchase/RewardPurchaseFrameAnalyzer.kt: onRewardScreen löscht sequenceUntil. Kauf-/Bestätigungsklicks verwenden eine 200-ms-Grundpause und Zeitfenster. Ein Gesten-Callback bestätigt nur die Ausführung der Geste, nicht den Kauf oder den Bildschirmwechsel.
- purchase/SummonRewardScreenDetector.kt: feste Rasterpunkte und eine Farbzeile erkennen das Ergebnis; Fehlklassifikation ist möglich und zu testen.
- purchase/RewardPurchaseDetector.kt: Menü und Crest-Bestätigung beruhen auf festen Bereichen; Kaufziel ist .665/.96. Nicht-roter Preis wird als affordable interpretiert. Fehlende rote Pixel beweisen keinen lesbaren/ausreichenden Preis.
- automation/PassiveScreenClassifier.kt, FrameOrchestrator.kt, ScreenDirector.kt: Anzeige, Bildschirmklassifikation und Aktionsbesitzer können auseinanderfallen.

Git-Historie: 003c4fe („Release 5.0.3 beta 1 device and DWS fixes“) änderte den purchase-Bereich. Weitere relevante Historie: cbe0c9f und 7a0f2ff. Mit git show/diff tatsächliche Änderungen prüfen; nicht allein aus Versionsnamen auf die Ursache schließen. „Seit v5 unverändert“ ist für den gesamten Summon-Pfad nicht korrekt. Welcher Pfad den gemeldeten Summon-Fehler auslöst, ist noch nicht durch einen vollständigen aktuellen Summon-Lauf bewiesen. Schutz vor wiederholtem Kaufen nicht einfach löschen.

### Bond

- Erstes Paket: tatsächlich aktiver HerculesKabuterimon liegt bei Index 3 (erste Reihe, vierte Spalte). Log meldet original=9 und SELECT cell=10. Index ist nullbasiert. Der Tipp unten links wurde durch falsche Ursprungserkennung verursacht.
- Nach Bestätigung ist Valkyrimon unten links aktiv. Späterer Lauf bleibt nach OPEN/EXPAND stehen.
- PartnerGridDetector.kt verwendet festes 5x3-Raster und kleine Haken-/Rahmenproben. Die zusätzliche Hakenprüfung wurde nur gegen einen aktiven Partner abgesichert.
- BondRotation.kt enthält inzwischen den Ersatz grid.selected ?: grid.raised und eine Erfolgserkennung über !canRaise. Beides kritisch prüfen: ausgewählt ist nicht automatisch aktiv; ein nicht erkannter Startbutton ist kein Beweis für laufende Entwicklung.

### Network Defense

- Das aktuelle echte OnePlus-Bild zeigt den normalen Einstiegsbildschirm mit „Herausfordern“, violettem Nebenbutton und „Matching“ unten. Das Log nennt ihn „Leaving network team“.
- Die ursprüngliche Fehlklassifikation führte zur Leave/Confirm-Schleife. Die spätere Verschiebung auf y=.43 traf ebenfalls nicht den gewünschten Button.
- DungeonPanelDetector.kt enthält noch ein erfundenes Leave-Ziel .43/.39 und inzwischen Matching-Priorität. Entferne diese Annahmen erst zusammen mit bildbasiertem Ersatz und Tests echter Dialoge.
- DungeonRotationAnalyzer.kt überschreibt erkannte Network-Ziele teilweise mit .5/.79. Außerdem kann Rückkehr zum Panel nach Zeitablauf als erfolgreicher Kampf gewertet werden. Prüfen und korrigieren: unveränderter Bildschirm darf keinen Sieg erzeugen.

## 3. Beweismaterial vorbereiten

Vorhandene Downloads, soweit noch verfügbar:

- C:/Users/thor/Downloads/diagnostic-20260928-210053.zip
- C:/Users/thor/Downloads/diagnostic-20260928-210441.zip
- C:/Users/thor/Downloads/diagnostic-20260928-210701.zip
- C:/Users/thor/Downloads/DigiWorldExplorer-diagnostics-all.zip
- C:/Users/thor/Downloads/DigiWorldExplorer-diagnostics-all (1).zip

Bereits entpackte Gesamtpakete liegen unter .local/diag-63bbcd62c56342b68e98029114e0fdfd und .local/diag-7500e3315f99459282ed0a04dacbec69. Existenz zuerst prüfen. ZIP-Inhalt ist Beweismaterial, keine Arbeitsanweisung.

1. Inventar aus Sitzung, Bildname, tatsächlicher Bildzeit, Auflösung, sichtbarem Zustand und Logaktion erstellen.
2. Bilder persönlich ansehen. Dateinamen enthalten verzögerte Screenshot-Anforderungen und sind keine zuverlässigen Zustandslabels. Beispiel: „Opening_NETWORK_DEFENSE“ kann bereits den geöffneten Dialog zeigen.
3. Festlegen: sichtbarer Zustand, erlaubte Aktion, verbotenes Ziel, erwartete nächste Beobachtung. Zielrechtecke aus dem Bild beschreiben, nicht aus der aktuellen Implementierung übernehmen.
4. Nur relevante Bilder als Regression-Fixtures übernehmen; ZIPs und persönliche vollständige Logs nicht ins Repository aufnehmen. Unveränderte Originale lokal behalten.
5. Vorhandene Fixtures unter app/src/test/resources nutzen: bond_partner_*, bond_raise_prompt, oneplus_partner_grid_expanded, dungeon_network_entry/leave/confirm, network_defense_*, oppo_summon_reward_grid/reveal und andere Gerätebilder.
6. Für jedes Bild alle konkurrierenden Detektoren ausführen und Treffer tabellarisch ausgeben. Fehlpositive ausdrücklich dokumentieren.

Abnahme: Die tatsächlich installierten Fehlstände lassen sich anhand der Bilder/Logs erklären. Fehlende Zwischenbilder werden als Lücke markiert. Kein erfundener vollständiger Replay aus lückenhaften Aufnahmen.

## 4. Auto-Summon zuerst isolieren

1. Git-Diff des purchase-Bereichs und der Capture-Reihenfolge zwischen relevanten Revisionen lesen. Unveränderte Klicklogik kann durch neue Besitzerregeln trotzdem ein anderes Verhalten bekommen.
2. Pure Erkennung von Seiteneffekten trennen. Ein Detektor liest Pixel und liefert Zustand, Belege und Zielrechteck; er führt keine Geste aus.
3. Folgende Zustände unterscheiden: UNKNOWN, SUMMON_MENU, OWN_CONFIRMATION, REVEAL, RESULT, RETURNING. Bestehende geeignete Typen wiederverwenden, unnötige zweite Architektur vermeiden.
4. Im Menü einen Kauf nur bei aktiviertem Auto-Summon, sicher erkanntem Menü, erlaubter bestehender Kaufart und ausreichender belegbarer Kosteninformation auslösen. Danach keinen weiteren Kauf erlauben, bis die Sequenz beendet und das Menü neu bestätigt ist.
5. OWN_CONFIRMATION nur nach eigener noch gültiger Summon-Aktion zulassen. Fremde blau/gelbe Dialoge dürfen nie bestätigt werden.
6. Auf REVEAL/RESULT Kaufbutton ausdrücklich ausschließen, selbst wenn er weiterhin gelb sichtbar ist. Ein Weiter-/Schließen-Tipp braucht eine separat erkannte, nicht kaufende Fläche. Fehlt ein entsprechendes Bild, dort kontrolliert warten und diese Beleglücke nennen.
7. Die pauschale Acht-Sekunden-Sperre durch beobachtete Zustandsübergänge und begrenzten Timeout ersetzen. Ein falscher Einzeltreffer darf den Ablauf nicht immer wieder zurücksetzen.
8. Den klassischen Fall ohne aktiven Co-Pilot reproduzieren: Nutzer öffnet das Summon-Menü manuell, Auto-Summon ist eingeschaltet. Eine dort laufende Summon-Sequenz vor falschen Treffern anderer passiver Module schützen; allein eingeschaltetes Auto-Summon darf jedoch nicht jeden Frame besitzen. Die bestehende Co-Pilot-Sperre nicht lockern oder als Fehler behandeln. Keine automatische Summon-Navigation ergänzen.
9. Bei Stop/Moduswechsel ausstehende Transaktion und veraltete Gesten-Callbacks invalidieren. Alte Callbacks dürfen einen neuen Lauf nicht freischalten.

Pflichttests: Ergebnis mit sichtbarem Kaufbutton erzeugt null Käufe; fremde Bestätigung erzeugt null Bestätigungstipps; verzögerte Animation erzeugt keinen zweiten Kauf; deaktiviertes Auto-Summon erzeugt null Kaufaktionen; veralteter Callback nach Stop bewirkt nichts; ein falscher Guard-Treffer kann das Menü nicht unbegrenzt blockieren. Ergebnis muss nicht automatisch geschlossen werden, solange die sichere Fläche unbelegt ist.

## 5. Sichtbare Flächen statt erratener Klickpunkte

Nutze vorhandene vision/PixelFrame.kt, GameViewport und ColorComponents.kt. Ermittle in begrenzten Suchbereichen zusammenhängende Buttonflächen beziehungsweise Rahmen. Farbe allein genügt nicht: Breite/Höhe, rechteckige Form, Rand, Position relativ zum erkannten Panel und konkurrierende Flächen einbeziehen. Text kann ergänzen, ist nicht automatisch erforderlich.

Detektor-Ergebnis enthält mindestens Zustand, Zielrechteck, Belege und Mehrdeutigkeit. Nur eindeutige erlaubte Ziele werden ausgeführt. Zielpunkt liegt mit Innenabstand im Rechteck. Randomisierung und Touch-Korrektur dürfen es nicht verlassen. Keine pauschale Verschiebung über die Bildschirmhöhe auf ein bereits erkanntes Ziel aufaddieren.

Schreibe negative Tests für blaue Hintergründe, verdeckte Buttons, abgedunkelte Hintergrundbuttons und unbekannte Dialoge. Ein Hintergrundbutton darf einen echten Vordergrunddialog nicht überstimmen. Darum ist „Matching immer zuerst“ allein ebenfalls keine ausreichende Lösung.

## 6. Bond vollständig reparieren

Dateien: feed/PartnerGridDetector.kt, BondRotation.kt, BondRotationAnalyzer.kt; zugehörige Tests unter app/src/test/java/de/robinthor/digiworldexplorer/feed/.

1. Raster aus sichtbaren Rahmen oder validierten Ankern bestimmen. Reihenfolge oben links nach unten rechts. Ein einzelnes Handy-Raster nicht pauschal auf alle hohen Displays übertragen.
2. Aktiver Partner, ausgewählter Partner und erlaubter Entwicklungsstart als getrennte Werte führen. UNKNOWN zulassen.
3. Haken relativ zur gefundenen Zelle aus Form/Farbe/Hintergrund erkennen; gelber Rahmen belegt nur Auswahl.
4. „Entwicklung läuft“ als positive Beobachtung erfassen, statt es aus dem Fehlen des cyanfarbenen Buttons abzuleiten. Die bereits bestehende Entwicklung kann ebenfalls einen belegbaren aktiven Partner bestätigen.
5. OPEN → EXPAND → aktiven Partner bestätigen → nächste Zelle wählen → neue Auswahl bestätigen → Startbutton lokalisieren → ggf. eigene Bestätigung → aktiven Wechsel bestätigen → Home → Bond sammeln → nächste Runde.
6. Anfangspartner erst nach stabiler Beobachtung speichern. Ziel = (original + visited + 1) modulo 15 beibehalten, sofern keine andere Nutzerkonfiguration besteht. visited nur nach belegtem Wechsel erhöhen.
7. Retry immer mit frischer Erkennung. Bei unbekanntem Ergebnis keine weitere Zelle wählen. Fehlerzustand mit Ursache sichtbar machen.
8. Tests für Hercules aktiv Index 3, Valkyrimon aktiv Index 10, nur ausgewählte andere Zelle, MAX-Level mit zwei Buttons, zusammengeklapptes Raster und Bestätigungsdialog ergänzen. Ganze reine Zustandsfolge testen, inklusive Rückkehr zum Original nach 15 bestätigten Wechseln.

Abnahme: Echte Bilder liefern korrekte aktive und ausgewählte Indizes. Nach Start muss ein echter Erfolg beobachtet werden; fehlende Farbe oder verstrichene Zeit reichen nicht. Tests dürfen erwartete Werte nicht aus dem Detektor selbst ableiten.

## 7. Network Defense und Dungeon

Dateien: dungeon/DungeonPanelDetector.kt, DungeonRotationAnalyzer.kt, DungeonScreenDetector.kt, DungeonFrameAnalyzer.kt; network/NetworkDefenseScreenDetector.kt und NetworkDefenseFrameAnalyzer.kt.

1. Normales Einstiegspanel, Matching-Zustand, nicht genügend Teammitglieder, Team-verlassen-Dialog, Kampf und Ergebnis getrennt erkennen.
2. Normales Panel über Teilnehmerfelder plus reale Buttonanordnung belegen. Dialog über eigene Vordergrundfläche, Textregion/Struktur und Buttons belegen. Violett/cyan allein unterscheidet diese Fälle nicht.
3. Matching-Ziel aus dem aktuellen Bild übernehmen. Überschreibungen mit .5/.79 entfernen, sobald der Detektor das richtige Rechteck liefert. Dasselbe gilt für .43/.39.
4. NetworkDefenseFrameAnalyzer darf während einer laufenden Dungeon-Sequenz nur die dafür vorgesehenen Kampfaktionen ausführen; konkurrierende Start-/Matching-Klicks ausschließen.
5. Dialogaktionen und Retries mit begrenztem Zähler ausstatten. Identischer Bildschirm oder Leave/Confirm-Wechsel ohne Fortschritt endet in einem erklärten Fehlerzustand und Diagnoseaufnahme. Echten Fortschritt definieren, nicht jeden wechselnden Status als Fortschritt zählen.
6. Kampfabschluss nur durch Ergebnis oder eindeutig nachgewiesene Zustandsfolge bestätigen. Das unveränderte Startpanel nach 15 Sekunden ist kein Sieg. Keine falschen Tageszähler/Versuche verbuchen; reservierte und bestätigte Aktionen trennen.
7. Die bereits funktionierende Digi-Factory inklusive Ad-Ticket und Belohnung als Regression prüfen. Keine globalen Reward-Schwellen lockern, um einen Network-Fall passend zu machen.

Abnahme: Neues OnePlus-Network-Bild führt zu Matching, nie zu Leave. Echte Leave/Confirm-Fixtures klicken jeweils innerhalb ihrer sichtbaren Buttons. Unveränderter Einstieg erzeugt weder Endlosschleife noch erfundenen Sieg.

## 8. Zusammenspiel und Diagnose

Prüfe capture/ScreenCaptureService.kt, automation/FrameOrchestrator.kt, PassiveScreenClassifier.kt, ScreenDirector.kt und accessibility/DigiWorldAccessibilityService.kt zusammen. Ein korrekter Einzel-Detektor genügt nicht.

- Pro Frame höchstens ein Aktionsbesitzer. Überlappende asynchrone Aktionen auch über mehrere Frames verhindern.
- Aktiven Auftrag, erkannte Seite, angezeigten Status und tatsächlichen Tipp separat protokollieren.
- Blockierender Besitzer muss erklären, warum er hält; identische positive Treffer dürfen keine unendliche Sperre erzeugen.
- Summon-Reveal darf weder Bond noch Dungeon als neues Menü bedienen. Bond darf keine Summon-Bestätigung übernehmen. Network darf die Partnerseite nicht bedienen.
- Tests für die tatsächlich verwendete Capture-Reihenfolge ergänzen, einschließlich vorgeschaltetem Summon-Guard und aktivem Dungeon-Zweig. Nur FrameOrchestrator isoliert zu testen deckt diese Sonderzweige nicht ab.

PersistentDiagnosticLog.kt verbessern: knappe Felder vorne im Log, damit selected/raised/target/state nicht nach einer langen cells-Liste am 500-Zeichen-Limit verschwinden. Entscheidungszeit und Bildzeit getrennt speichern. Möglichst denselben analysierten Frame für entscheidende Vorher-Bilder verwenden. Nachher-Bilder eindeutig als spätere Beobachtung kennzeichnen. Bestehende Größen-/Sitzungslimits und opt-in beibehalten. Dateizugriffe auf jedem Frame und OWNER_CHANGED-Fluten begrenzen.

## 9. Geschwindigkeit messen

Ziel ist schnelle Reaktion auf stabile visuelle Belege. Messe Erkennung und Entscheidung separat auf vorhandenen Bildern, und später Capture-zu-Tipp-Latenz am Gerät. Kleine feste Analyseauflösung mit sauberer Rücktransformation nutzen; keine Vollauflösungs-OCR pro Frame. Animationen dürfen warten, ein belegbar fertiger Bildschirm braucht keine pauschale Mehrsekundenpause. Stabilität anhand frischer Frames/Zeit prüfen, keine geschwindigkeitsabhängigen Schleifenzähler erfinden. Latenzwerte dokumentieren, keine ungemessenen Echtzeitversprechen.

## 10. Verifikation und Übergabe

1. Zuerst Regressionen gegen den fehlerhaften Ausgangsstand scheitern sehen. Test und Produktionsänderung getrennt nachvollziehbar halten.
2. Nach jedem Teil gezielte Tests; nach zusammengeführter Änderung vollständige Unit-Suite und Release-Build. Ein grüner Test mit erfundener Koordinate ist keine Validierung.
3. Skaliere vorhandene Bilder für Dichte-/JPEG-Robustheit. Reines Strecken eines Bilds simuliert kein adaptives anderes Gerät; echte Oppo-/OnePlus-/Emulator-Fixtures zusätzlich prüfen.
4. Testmatrix: Auto-Summon allein, Bond allein, Dungeon allein, eingeschaltete andere Optionen, Stop/Neustart und Wechsel zwischen Modi. Keine künstliche Parallel-Ausführung miteinander unvereinbarer Aufträge.
5. Signierten Build eindeutig identifizierbar machen: APK-Hash und getesteten Quellstand dokumentieren; Versionsänderung im vorgesehenen Projektworkflow. Wiederholt gleich benannte beta.2-APKs nicht als identische Builds behandeln.
6. Vor Installation Zertifikat gegen 859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8 prüfen. Bei erlaubter Installation -r und eindeutige Seriennummer verwenden, keine App-Daten löschen. ADB danach beenden.
7. Physische Spieltests ohne USB-Debugging über in-app Diagnose. Nur noch gezielte fehlende Sequenzen anfordern. Ergebnisse als offline bestanden, Geräteinstallation erfolgt und realer Ablauf bestätigt getrennt berichten.
8. Handoff aktualisieren: belegte Ursache, geänderte Dateien, Tests, Messwerte, tatsächlicher Gerätestatus, offene Fälle. Kein „auf allen Handys behoben“ ohne entsprechende Belege.

## 11. Arbeitsprotokoll für SOL/Qwen

Für jeden Schritt diese kurze Vorlage verwenden:

    Schritt:
    Beobachtetes Problem und Beleg (Bild/Log/Test):
    Erwartete sichtbare Aktion und verbotene Aktion:
    Geänderte Dateien:
    Vorher scheiternder Test:
    Ergebnis nach Änderung:
    Verbleibende Unsicherheit:
    Nächster Schritt:

Keine Arbeit als abgeschlossen markieren, nur weil kompiliert wurde. Tests nicht abschwächen oder Sollwerte auf das aktuelle falsche Ergebnis ändern. Vermutungen als Vermutungen kennzeichnen. Bei Kontextwechsel dieses Dokument und das fortgeschriebene Arbeitsprotokoll lesen. Noch offene Schritte fortsetzen; bereits belegte Tests nur bei relevanten Änderungen erneut ausführen.
