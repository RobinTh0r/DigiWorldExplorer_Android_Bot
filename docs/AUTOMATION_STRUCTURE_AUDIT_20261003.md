# Erkennung und Ablaufsteuerung – 3. Oktober 2026

## Belegte Fehler und Vergleich

Basis: v5.3.0-beta.1, v4.0.0 und die bereits geprüfte öffentliche DigiAutoTap-Referenz
`eb65b2587bf5d9158bdcb7490b89c6a4a868ca1f` (`BondTour.kt`). Die Implementierung hier ist eigenständig.

Das neue Paket `diagnostic-20261003-160518` stammt von OnePlus CPH2611, Android 16.
Die Partnerseite und das Raster sind bereits offen. Der Bot meldet trotzdem `page=false` und
tippt viermal bei 124/2078 auf den bereits ausgewählten Tab. Die dünnen, fest positionierten
Messstreifen am großen Partnerrahmen verfehlen dessen tatsächliche Kanten. Es handelt sich nicht
um fehlende Wartezeit. Der Nutzer korrigierte seine erste Rückschrittmeldung: 5.2 und Beta 1 bleiben
an derselben Stelle hängen. Das Paket 160618 enthält nur Sitzungs-/Serviceereignisse, keine Spielszenen;
der Gesamtexport enthält dieselben beiden Sitzungen.

DigiAutoTap bestimmt vor einem Schritt dessen sichtbare Voraussetzungen, sucht Bedienelemente und
prüft nach dem Tippen den neuen Zustand. Sein Bond-Ablauf wählt den Partner-Tab gezielt und klappt
das Raster nur aus, wenn es tatsächlich eingeklappt ist. Bekannte störende Dialoge haben eigene
Rückwege. Auch die Referenz begrenzt Wiederholungen; allein mehr Wiederholungen lösen unser Problem nicht.

## Umgesetzt nach Beta 1

- `PartnerVisualLocator` findet zusammenhängende farbige Rahmen im aktuellen Bild. Aus wiederholt
  beobachteten Kartenkanten bestimmt er Spalten und Reihen und prüft jede abgeleitete Zelle erneut
  auf ihren sichtbaren Rand. Layouts werden nicht nach Hersteller oder Modell ausgewählt.
- Gelbe Auswahlrahmen werden an mehreren Kanten der gefundenen Karte geprüft; ein animierter oder
  unterbrochener Rand muss nicht mehr einen einzelnen festen Messstreifen treffen. Aktiver Partner
  und ausgewählte Karte bleiben getrennte Zustände.
- Der Raise-Button wird zusätzlich innerhalb des gefundenen großen Partnerpanels gesucht.
- Expand sucht ein sichtbares Plus mit vier Armen statt eines festen Klickpunkts. Bestätigen
  benötigt zwei ausgerichtete violette/cyanfarbene Dialogknöpfe und klickt deren gemessene Mitte.
  Ohne passenden Bildnachweis wird dort nicht geklickt; Expand wiederholt höchstens zweimal
  und nur bei weiterhin sichtbarem Plus.
- Nach dem Partner-Tab-Wechsel entscheidet das beobachtete Raster, ob ein Expand-Schritt notwendig
  ist. Ein bereits ausgeklapptes Raster führt direkt zur nächsten Karte statt zu einem Minus-Tap.
- `FrameOrchestrator` kann eine laufende Sitzung über unerkannte Übergangsbilder hinweg besitzen.
  Bond und Network benutzen das vor den übrigen Modulen. Das verhindert, dass Titel, Summon oder
  andere Module in deren laufende Übergänge tippen. Die Bond-Sammlung delegiert Stage-Failed selbst.
- Bond protokolliert beobachtete Zustandsänderungen und den konkreten Zustand beim Abbruch. Home
  und Partner verwenden innerhalb des Bond-Schritts dieselbe Partnerbeobachtung.
- Die Overlay-Anzeige übernimmt die Beobachtung des Bond-Moduls; sie startet nicht erneut mehrere
  vollständige Raster-Suchen. Der angezeigte Zustand autorisiert selbst keinen Klick.

## Alte Funktionen: tatsächlicher Stand

| Funktion | Vergleich mit v4 / derzeitige Grundlage | Weiterer Prüfpunkt |
| --- | --- | --- |
| Auto-Summon | `RewardPurchaseDetector` ist gegenüber v4.0.0 funktional identisch; der Analyzer hat zusätzlich einen Status-Hook für den Scheduler. | Erkennung und schnelle bestehende Klickfolge als Regression erhalten. |
| Network Defense | Bestehende Start-/Kampf-/Boss-Erkennung, ergänzt um Sitzungszuständigkeit. | Echte Boss-Banner und geöffnete Karten als verschiedene Zustände absichern. |
| Klassisches Feed/Bond | Gemeinsame Home-/Bubble-Erkennung, zusätzliche Rotationsbestätigung seit v5. | Dialog-Rückwege und zeitliche Folgen aus echten Aufnahmen testen. |
| VS/Tower | Eigener Erkenner, Abschaltregel und Ergebnisbehandlung. | Menü/Kampf/Ergebnisfolgen prüfen, tägliche Dungeon-Rotation getrennt davon behandeln. |
| Digital World | V3/V4/V5-Profile, dynamisches Grid und Dash-Zähler. | Bewegung und Bildschirmnavigation nicht durch generische Menüerkennung unterbrechen. |
| Farm/Dungeon-Rotation | Neuere Module mit mehreren Bildankern und eigenen Zuständen. | Gemeinsame Beobachtungen und Aktionsergebnisse schrittweise übernehmen. |

## Noch keine vollständige Umstellung aller Module

Mehrere Erkenner verwenden weiterhin Prozentbereiche und die bisherigen Layout-Fallbacks. Auch
Partner-Tab und Home-Navigation haben noch feste Zielkoordinaten; für Expand und Bestätigen wurden
diese entfernt. Für Buddy/Support fehlen noch echte Diagnosebilder zur visuellen Tab-Suche. `PassiveScreenClassifier`
ist weiterhin eine Anzeige-/Fallback-Erkennung, keine vollständige zentrale Autorisierung aller Taps.
Die gesamte App darf deshalb nicht als vollständig dynamisch oder als DigiAutoTap-Parität bezeichnet werden.

Der nächste Umbau muss pro Modul dieselbe Schnittstelle verwenden: ein unveränderliches Bild mit
Zeitstempel und Spielfläche, gefundene Elemente mit Grenzen und Nachweis, eine zuständige Aufgabe,
danach genau eine erlaubte Aktion mit erwartbarem Ergebnis. Ein Retry verlangt erneut passende
Ausgangsevidenz; ein falscher Dialog verlangt einen benannten Rückweg. Ausgangsbild, Ziel und
Folgebild gehören in die Diagnose. Die Umstellung muss durch aufgezeichnete Abläufe mit negativen
Beispielen begleitet werden, bevor ein bisher funktionierender Classic-Pfad ersetzt wird.

## Verifikation

Der neue Partner-Screenshot wird als echter Eingabefall bis zum SELECT-Befehl abgespielt, sowohl
beim direkten Einstieg als auch nach einem Buddy/Partner-Wechsel. Tests umfassen bisherige
Partnerbilder, mehrere Capture-Auflösungen und kleine Layoutverschiebungen. Die Zuständigkeit wird
mit einer Folge aus erkannten, unbekannten und verdeckten Bildern gegen falsche Titel-/Summon-Taps geprüft.
Skalierte Aufnahmen ersetzen keinen Live-Test auf weiteren Handys.

Nextcloud hat während der Prüfung wiederholt Gradle-Ausgaben beschädigt. Für reproduzierbare
Prüfungen wurden Build-Ausgaben und Projektcache über ein lokales Gradle-Init-Skript nach
`C:/Users/thor/AppData/Local/Temp/dwe-verify-20261003` umgeleitet. Projekt-, App- und Signieridentität bleiben gleich.
