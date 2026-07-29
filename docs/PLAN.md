# Plan: DigiWorldExplorer Android

## Kurzurteil

Eine Android-App ist realistisch. Die Erkennung und Wegplanung lassen sich aus dem Python-Bot fachlich übertragen, der technische Unterbau muss jedoch nativ neu gebaut werden. Die empfohlene erste Ausgabe ist eine intern verteilte Guild-Beta-APK, nicht sofort eine Play-Store-App.

`PyGetWindow` löst nur die Desktop-/Emulatorseite: Es kann Fensterposition und -größe eines PC-Fensters ermitteln. Auf einem echten Android-Gerät wird stattdessen die tatsächliche Capture-Größe aus `MediaProjection` beziehungsweise `WindowMetrics` verwendet. Das Raster sollte weiterhin aus dem Bild erkannt und alle Klickpunkte relativ zu den erkannten Rastergrenzen berechnet werden. Dadurch wird die Lösung unabhängig von Auflösung, DPI, Seitenverhältnis und Letterboxing.

## Empfohlene Architektur

### Android-App

- Kotlin und Jetpack Compose für Oberfläche und Einstellungen
- `MediaProjection` in einem sichtbaren Foreground Service für Bildschirmbilder
- `AccessibilityService.dispatchGesture()` für Taps
- `TYPE_ACCESSIBILITY_OVERLAY` oder eine kleine schwebende Steuerung für Start, Pause, Stopp und Status
- lokale Bildverarbeitung, zunächst mit OpenCV Android
- keine Cloudübertragung und keine Spiel-APK-Modifikation

### Wiederverwendbare Fachlogik

Aus dem PC-Bot werden Verhalten und Tests portiert, nicht der ADB-Unterbau:

- dynamische Erkennung der sechs Rasterlinien
- Zellklassifikation und Position der Spielfigur
- Zielauswahl, Hindernisprüfung und sichere Folgeschritte
- Erkennung von Angriff/Dash/Tutorial-Zuständen
- Energiezähler und Laufstatistik

Die Koordinatenpipeline lautet:

`Capture-Bild -> erkannte Spielfläche -> Rasterzelle -> Bildschirmkoordinate -> Accessibility-Geste`

Rotation, Insets, Navigationsleiste, Cutouts und mögliche schwarze Ränder werden vor jedem Tap in der Transformation berücksichtigt.

## Berechtigungen und Nutzerfluss

1. App öffnen und Schritte festlegen.
2. Nutzer aktiviert den Bedienungshilfedienst bewusst in den Android-Einstellungen.
3. Nutzer bestätigt Androids Bildschirmfreigabe für `MediaProjection`.
4. Foreground-Service zeigt eine dauerhafte Benachrichtigung.
5. Overlay bietet jederzeit Pause und Stopp.

Die Bildschirmfreigabe darf nicht heimlich umgangen werden. Für aktuelle Android-Versionen muss die Capture-Sitzung als Media-Projection-Foreground-Service laufen. Der AccessibilityService muss seine Gestenfähigkeit deklarieren.

## Play Store und Verteilung

Für eine private Testgruppe ist eine signierte APK per GitHub Release der pragmatische Weg. Im Play Store muss die Accessibility-Nutzung deklariert, deutlich erklärt und vom Nutzer bestätigt werden. Googles Richtlinie erlaubt eng begrenzte, deterministische Automatisierung eher als frei planende autonome Aktionen, dennoch bleibt ein Spielbot ein relevantes Prüf- und Ablehnungsrisiko. Zusätzlich sind die Regeln des jeweiligen Spiels zu Automation zu beachten.

Empfehlung:

- Phase 1: lokale Debug-APK nur für eigene Geräte
- Phase 2: signierte Guild-Beta über GitHub Release
- Phase 3: Play-Store-Eignung erst nach separater Policy- und ToS-Prüfung entscheiden

## Aufwandsschätzung

| Umfang | Realistische Größenordnung |
|---|---:|
| Technischer Prototyp: Capture, Overlay, ein Test-Tap | 2–4 Arbeitstage |
| MVP auf 1–2 bekannten Geräten | 1–2 Wochen |
| Robuste Guild-Beta für mehrere Auflösungen/Hersteller | 3–6 Wochen |
| Store-reife App mit Onboarding, Telemetrie-freiem Fehlerbericht, breitem Gerätetest | 6–10+ Wochen |

Die größte Unsicherheit ist nicht die Oberfläche, sondern zuverlässiges Capture-Timing und Bilderkennung über unterschiedliche Geräte, Skalierungen, Spielversionen und Performanceklassen hinweg.

## Benötigt auf dem Entwicklungs-PC

- Windows 10/11 mit aktivierter Virtualisierung
- Android Studio (aktuelle stabile Version)
- Android SDK, Platform Tools und Build Tools
- JDK aus Android Studio
- Git
- mindestens ein echtes Android-Testgerät, idealerweise zusätzlich ein zweites Gerät mit anderem Seitenverhältnis
- USB-Kabel und aktiviertes USB-Debugging für Entwicklung/Logs
- optional Android-Emulator, aber ein echtes Gerät ist für Overlay, Capture und Herstellerbesonderheiten Pflicht

Empfohlen: 16 GB RAM, etwa 20–30 GB freier Speicher und ein GitHub-Repository mit GitHub Actions für Debug-/Release-Builds.

## Meilensteine

### M0 – Grundlagen und Datensatz

- Mindest-Android-Version und Testgeräte festlegen
- 30–50 anonymisierte Screenshots pro Zielauflösung sammeln
- aktuelle Erkennungstests und erwartete Raster/Ziele dokumentieren
- Spielregeln und zulässige interne Verteilung klären

### M1 – Technischer Spike

- leeres Kotlin-/Compose-Projekt
- MediaProjection-Foreground-Service
- Anzeige von Capture-Auflösung, Rotation und Insets
- Overlay mit Start/Pause/Stopp
- einzelner, vom Nutzer ausgelöster Test-Tap

Abnahmekriterium: Capture und Tap funktionieren nach sichtbarer Freigabe auf zwei Geräten, ohne feste Pixelkoordinaten.

### M2 – Bilderkennung

- Rastererkennung nach Kotlin/OpenCV portieren
- erkannte Zellen als Debug-Overlay zeichnen
- Offline-Tests mit gespeicherten Screenshots
- Koordinatentransformation für alle Testgeräte absichern

### M3 – Bot-MVP

- deterministische Ziel- und Bewegungslogik portieren
- Schritte, Fortschritt, Energie und Abschlussstatistik
- Sicherheitsstopps bei unsicherem Raster, App-Wechsel oder Capture-Verlust
- konfigurierbares Mindestintervall zwischen Aktionen

### M4 – Guild-Beta

- signierter Release-Build und installierbare APK
- verständliches Berechtigungs-Onboarding
- lokale Diagnoseexporte ohne Screenshots als Standard
- Testmatrix, Changelog und Rollback-Anleitung

## Erste offene Entscheidungen

- Mindestversion: vorläufig Android 10 oder neuer
- Zielgeräte und konkrete Android-Versionen
- nur Querformat oder Rotation unterstützen
- GitHub-Release/APK oder später Play Store
- Paketname, z. B. `de.robinthor.digiworldexplorer`
- ob OpenCV eingebettet wird oder eine kleinere eigene Bildpipeline genügt

## Nicht im Scope

- Patchen, Injecten oder Reverse Engineering der Spiel-APK
- Umgehen von Android-Sicherheitsdialogen
- Root-Zugriff
- verdeckte Eingaben oder Bildschirmaufzeichnung
- Cloudbasierte Verarbeitung von Screenshots

