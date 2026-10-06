# DigiAutoTap 1.3: Vergleich und Geräteplan (4. Oktober 2026)

Quelle: [DigiAutoTap 1.3, Tag `android-v1.3`](https://github.com/digiPr1me/DigiAutotap/releases/tag/android-v1.3), veröffentlicht am 4. Oktober 2026. Vergleichsstand hier: `v5.3.0-beta.1` plus die unveröffentlichten Änderungen in diesem Arbeitsverzeichnis. Die Veröffentlichung und der Quellcode von DigiAutoTap werden nur als technische Referenz gelesen; seine Lizenz erlaubt keine Übernahme oder Weiterveröffentlichung des Codes. Alle Änderungen hier sind eigenständig.

## Was 1.3 allgemein verbessert

1. Die Aufgaben-Kette kann nach einem unterbrochenen Schritt erneut anlaufen, erkennt begründete Stopps und überspringt nach begrenzten Versuchen einen festhängenden Schritt. App-Öffnen, Screenshots und kurze Home-Animationen sollen den aktiven Schritt nicht mehr verlieren.
2. Bond schließt ein versehentlich geöffnetes Digimon-Fenster und setzt einen unterbrochenen Rundgang fort. Dungeons zählen Siege, Niederlagen und Network-Defense-Starts genauer und tippen nicht blind in schwarze Zwischenbilder. Farm zählt Samen auch auf schmaleren Bildern und verteilt Wasserkannen nach Restzeit.
3. World Search liest die untere Reihe robuster, hält Dash bei Unsicherheit zurück und gibt das Raster nach dem Verlassen frei. Titelbilder bei Nacht, tägliche Nachrichten, Download- und Quest-Belohnungsfenster werden gesondert erkannt.
4. DigiAutoTap nennt jetzt auch Tablets, Faltgeräte und Querformat als unterstützte Bildschirmformen. Unbekannte Formen werden protokolliert. Das Log-ZIP enthält in 1.3 keine gespeicherten Spielbilder mehr.
5. Neu hinzugekommen sind unter anderem Chef's Special, EX Missions, Presets und bestimmte Ad-Skip-Pass-Rewards. Das sind zusätzliche Aufgaben, keine direkte Reparatur unserer bisherigen Funktionen. Sie werden hier nicht ungefragt in DigiWorldExplorer übernommen.

## Eigener Stand und konkrete Abweichungen

| Bereich | Bereits vorhanden | Noch offen oder belegt fehlerhaft |
| --- | --- | --- |
| Bond | Laufende Sitzung beansprucht alle Bilder; Raster, Auswahl, Plus, Raise und Bestätigen werden auf vorhandenen Aufnahmen visuell gelesen. | Der Partner-Tab und Home-Buttons haben noch feste Ziele. Es gibt noch keinen vollständigen Wiederanlauf mit Besuchsliste nach App-Wechsel. Ein echter Buddy/Support-Screenshot für die Tab-Lokalisierung fehlt. |
| Versehentliches Partner-Fenster | Neu: Partner-Detailkarte im OnePlus-Bild erkannt, höchstens zweimal pro Sammlung mit Android-Zurück geschlossen; Gegenbeispiele abgesichert. | Der Rückweg ist auf diese bekannte Fensterform begrenzt. Er muss auf dem echten Gerät erprobt werden. |
| World Search | Sichtbares Raster wird kalibriert, Zellen und Dash/HUD werden gelesen. Neu: während Kalibrierung keine konkurrierenden generischen Titel-Taps. | Eine gespeicherte Kalibrierung verwendet zeitweise alte Grenzen; Ausstieg und Dash-ohne-Vorrat brauchen weitere vollständige Ablaufaufnahmen. Querformat ist wegen der App-Ausrichtung nicht abgedeckt. |
| Dungeons/Network | Eigene Dungeon-Sitzung, sichtbare Panels und begrenzte Start-Wiederholungen. Network besitzt seine laufenden Bilder. | Zwei Berichte über frühes Verlassen des Dungeons haben noch keinen kompletten Diagnoseablauf. Ergebnis-/Ticket-/Belohnungsübergänge und schwarze Zwischenbilder pro Gerät absichern. |
| Farm | Ernte, Samenwahl, Pflanzen und Gießen besitzen eigene Leser und Tests. | Breite der Samen-/Zählerfelder und verspätete Dialoge über mehrere echte Geräte prüfen. |
| Auto-Summon Classic | V4-Erkenner und schnelle Klickfolge sind erhalten. | Vor generischen Probes priorisiert, aber sämtliche Zwischenbilder und Ticketfarben sollen als kompletter Replay getestet werden. |
| Bildschirmform | Lange Portrait-Handys nutzen die ganze Capture-Fläche; breite Bilder werden bisher als zentrierte 9:16-Fläche behandelt. | Diese Annahme ist kein universeller Geometrie-Erkenner. Manifest erzwingt Portrait. Tablet/Foldable/Querformat brauchen eigene gemessene Spielgrenzen und Tests; nicht bloß andere Prozentwerte. |
| Diagnose | Freiwillig aktivierte, begrenzte Screenshots plus Ereignisse und ZIP-Export. | Bild und Entscheidung sollten für jede problematische Aktion zusammenliegen. Bilder bleiben bewusst erhalten, weil sie für diese Gerätefehler vom Nutzer verlangt wurden. |

## Umsetzungsfolge

1. **Eigentümer und Folgebeweis vereinheitlichen.** Jeder aktive Ablauf hält die Bildzuständigkeit bis zum bestätigten Ergebnis oder benannten Abbruch. Pro Geste werden vorheriges Bild, gefundenes Ziel, erwarteter nächster Zustand, Frist und Folgebild protokolliert. Unbekannte Bilder erhalten eine kurze zweite Prüfung, bevor ein anderer Ablauf sie übernehmen darf. Der World-Search-Titelschutz und der Partner-Fenster-Rückweg sind erste lokale Schritte davon.
2. **Viewport aus Spielankern bestimmen.** Startpunkt sind Rahmen, Home-Navigation, Raster oder Dialogkontur; daraus folgen tatsächliche Spielgrenzen und sichere Klickflächen. Die `GameViewport.fit`-Annahme bleibt nur Rückfall für bereits geprüfte Portraitformen. Bei unbekannter Form: Diagnose statt geratenem Tap. Erst nach Fixture-Tests wird Tablet/Foldable/Querformat aktiviert.
3. **Bond Ende zu Ende abspielen.** Aufnahmen: Home-Bubble, falsches Partner-Detailfenster, Buddy/Support-Tab mit Marker, eingeklapptes und offenes Raster, Plus/Minus, 15 Wechsel, Bestätigung, Rückkehr zu Home, Sammeln, Wiederaufnahme nach App-Wechsel. Aktiven Partner und bereits besuchte IDs über Unterbrechungen erhalten. Jede Aktion braucht ein sichtbares Folgeresultat.
4. **Dungeon und Farm getrennt migrieren.** Dungeons zuerst: Ticketstand, Matching, Ad-Skip, schwarze Kampfphase, Sieg/Niederlage, Belohnung, nächster Lauf, Rückkehr. Farm: leeres Feld, Samenarten und Zähler, Kauf-/Ad-Dialoge, Wasserrestzeit. Für jeden Übergang positive und ähnlich aussehende negative Screens. Classic-Summon bleibt bis zu seinem Replay unangetastet.
5. **Gerätematrix laufen lassen.** OnePlus 8 Pro und CPH2611, Samsung S21 Ultra und S22-Diagnosegerät, Xiaomi-Meldung sowie mehrere Capture-Größen. Pro Gerät vollständige, zusammenhängende Diagnose vom Start bis zum Ergebnis. Bei der bekannten Spielsperre `00000038` USB-Debugging vor dem Game-Run ausschalten und die App-Diagnose verwenden. Kein Modell gilt wegen skalierter Testbilder allein als bestätigt.
6. **Freigabekriterium.** Keine konkurrierenden Taps, keine geratenen Ziele bei unbekanntem Zustand, 15/15 Bond mit Wiederherstellung, Dungeon-Ticket-/Belohnungszählung und Farm-Pflanz-/Gießablauf auf den vorhandenen echten Geräten. Erst danach Versionsänderung und Veröffentlichung auf ausdrücklichen Wunsch.

## Diagnosebedarf

- Ein Paket mit aktivierter Diagnose vom Öffnen des Digimon-Menüs auf dem **Buddy- oder Support-Tab mit rotem Marker** bis zum Umschalten auf Partner.
- Ein Paket vom **Start der Dungeon-Rotation** bis zu dem Moment, in dem sie trotz verbleibender Versuche Home öffnet; inklusive Einstellungen für Ad-Skip und Amy.
- Ein vollständiger **Meat-Field-Ablauf** auf dem Gerät, auf dem Samenauswahl oder Belohnung nicht angenommen wird.

Die ZIPs `diagnostic-20261003-160518` und `...160618` enthalten den Partner-Rasterstillstand bzw. nur Serviceereignisse; der Gesamtexport dupliziert diese Sitzungen. Sie belegen keinen Dungeon-Ablauf und keine Buddy-Tab-Geometrie.
