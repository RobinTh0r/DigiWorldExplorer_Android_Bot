# ⚡ DigiWorldExplorer Android Bot ⚡

![Version](https://img.shields.io/badge/version-0.2.3-yellow) ![Status](https://img.shields.io/badge/status-beta-orange) ![Platform](https://img.shields.io/badge/platform-Android%2010%2B-green)

### 🦖 Native Android-Automatisierung für Digimon UP

✨ RobinTh0r Guild Edition · Exclusive for Germon Members ✨

`Lokal` · `Deterministisch` · `Bedienungshilfe` · `Keine Cloud-KI` · `Safety first`

> [!WARNING]
> Dieses private Fanprojekt ist nicht mit den Entwicklern von Digimon UP
> verbunden. Spielautomatisierung kann gegen Spielregeln verstoßen. Nutzung
> ausschließlich auf eigene Verantwortung und ohne Gewährleistung.

> [!NOTE]
> 🔗 Schwesterprojekt: [DigiWorldExplorer_Bot](https://github.com/RobinTh0r/DigiWorldExplorer_Bot)
> automatisiert dasselbe Spiel über BlueStacks unter Windows/Python. Diese App
> ist die native Android-Portierung ohne PC, BlueStacks oder ADB-Abhängigkeit.

## 🌟 Was macht die App?

Die App läuft direkt auf dem Android-Gerät und benötigt weder PC noch
BlueStacks. Sie erfasst den Bildschirm lokal über `MediaProjection`, erkennt
das sichtbare 5×5-Raster, die Spielfigur, Items, Pyramiden sowie die
HUD-Zähler (Krallen, Dash) und plant sichere Bewegungen. Alle Eingaben laufen
über einen selbst aktivierten `AccessibilityService` – es gibt keine feste
Bildschirmkoordinate und keine Modifikation der Spiel-APK.

Priorität bei der Wegwahl:

1. 🟠🟣🟢 Erreichbare Items auf dem kürzesten Weg
2. ➡️ Sichere Erkundung nach rechts, mehrere Schritte im Voraus geplant
3. 🔺 Umwege statt Zerschlagen – Pyramiden werden nur mit Krallen-Reserve genommen
4. 💨 Dash nur bei mehreren Hindernissen in Folge und ausreichend Ladungen

## 🛡️ Sicherheitsprinzip

- 📸 Vor jeder Entscheidung wird das aktuelle Kamerabild neu ausgewertet.
- 🧭 Keine festen Bildschirmkoordinaten – alles relativ zum erkannten Raster.
- 🛑 Bei unsicherem Raster, unklarer Spielererkennung oder eingeblendeter
  Spielmeldung wird gewartet statt geraten.
- 🔢 HUD-Zähler werden nur bei sicherer Erkennung verwendet; unbekannte Werte
  gelten als "fast leer" und lösen vorsichtiges Verhalten aus.
- 🔁 Automatik ist jederzeit über die App, die Android-Benachrichtigung oder
  Wegwischen der App stoppbar.
- ☁️ Keine Cloud-API und kein KI-Modell während der Laufzeit.

## 🚀 Schnellstart

### Voraussetzungen

- Android 10 (API 29) oder neuer
- Digimon UP installiert

> [!TIP]
> Empfehlung für den Betatest: Verwendet möglichst **Botamon**. Sein kleiner,
> farblich klarer Sprite lässt sich aktuell am zuverlässigsten erkennen –
> die App zeigt dieselbe Empfehlung auch direkt im Steuerbildschirm. Andere
> Digimon-Formen können funktionieren, sind aber noch nicht gleich gut
> kalibriert.

### Installation

1. APK aus [Releases](../../releases) herunterladen.
2. Installation aus unbekannter Quelle einmalig erlauben.
3. App öffnen und der Reihe nach:
   1. Bildschirmfreigabe starten
   2. Bedienungshilfe öffnen und die App aktivieren
   3. Raster über anderen Apps erlauben
   4. Automatik starten
4. Digimon UP in den Vordergrund holen – das Raster-Overlay erscheint über
   dem Spiel.

## 🎮 Bedienung

Die Steuer-UI zeigt Status, Rasterkontrolle und Version in einer Ansicht:

1. **Bildschirmfreigabe starten** – erlaubt der App, den Bildschirm lokal via
   `MediaProjection` zu lesen. Ohne diesen Schritt läuft keine Analyse.
2. **Bedienungshilfe öffnen** – aktiviert den `AccessibilityService`, über den
   alle Taps gesendet werden. Einmalig pro Installation nötig.
3. **Raster über anderen Apps erlauben** – Overlay-Berechtigung für das
   Debug-Raster, das während der Automatik über dem Spiel eingeblendet wird.
4. **AUTOMATIK STARTEN** – ab jetzt entscheidet und tippt die App selbst.
   Jederzeit über **AUTOMATIK SOFORT STOPPEN**, die Android-Benachrichtigung
   oder Wegwischen der App unterbrechbar.

Das Raster-Overlay lässt sich separat ein-/ausblenden, ohne die Automatik zu
stoppen – nützlich, um kurz das reine Spielbild zu sehen.

## 🧠 Entscheidungsablauf

```
MediaProjection-Screenshot
      ↓
5×5-Raster automatisch erkennen
      ↓
Spieler, Items, Wege, Pyramiden und HUD-Zähler bewerten
      ↓
Sicherste Aktion relativ zum Raster wählen (ggf. mehrere Schritte gebündelt)
      ↓
Bedienungshilfe-Geste senden
      ↓
Wirkung und neuen Zustand erneut prüfen
```

## 📂 Projektstruktur

| Pfad | Zweck |
| --- | --- |
| `app/src/main/java/.../MainActivity.kt` | Steuer-UI: Freigaben, Start/Stopp, Version, Spenden-Link |
| `app/src/main/java/.../capture/` | Bildschirmaufnahme und Frame-Analyse |
| `app/src/main/java/.../detection/` | Raster-, Spieler-, Item- und HUD-Erkennung |
| `app/src/main/java/.../strategy/` | Bewegungsplanung, Automatik-Zustand |
| `app/src/main/java/.../accessibility/` | AccessibilityService inkl. Debug-Overlay |
| `app/src/test/` | Offline-Regressionstests (JVM, keine Geräteverbindung nötig) |
| `keystore.properties.example` | Vorlage für die lokale Release-Signierung |

## 🧪 Offline testen

```powershell
./gradlew testDebugUnitTest
```

Diese Tests senden keine Gesten und benötigen kein Gerät.

## 🧯 Häufige Probleme

| Problem | Lösung |
| --- | --- |
| Bildschirmfreigabe-Dialog erscheint jedes Mal neu | Android verlangt das bei jedem Neustart der Analyse – erneut bestätigen |
| Bedienungshilfe wird nach Update/Neuinstallation deaktiviert | Einmalig unter Einstellungen → Bedienungshilfe wieder aktivieren |
| Raster sitzt nicht über dem Spielfeld | Digimon UP in den Vordergrund holen, App zeigt "Raster unsicher" bis erkannt |
| Automatik tippt daneben, Spiel zeigt Fehlermeldung | Kurz warten – die App pausiert automatisch, bis die Meldung verschwindet |
| Automatik reagiert gar nicht | Prüfen, ob Bildschirmfreigabe UND Bedienungshilfe beide aktiv sind |
| Krallen/Dash-Zähler zeigt "?" | Ziffer aktuell nicht sicher lesbar – App behandelt sie vorsichtshalber als "fast leer" |

## 📝 Versionen und Changelog

Die aktuelle Version steht in `app/build.gradle.kts` (`versionName`) und wird
unten in der App-Steuerung angezeigt.

### Unreleased

- Noch keine Änderungen.

### v0.2.3 – 31.07.2026

- ⏱️ Klick-Rhythmus auf 800 ms eingestellt: pro Tap eine feste Pause, sauberere Bildanalyse
- 🚫 Burst-Taps brechen ab, wenn zwischendurch eine Fehlermeldung im Bild erscheint

### v0.2.2 – 31.07.2026

- 💡 Hinweis in der App und der README: Botamon wird für die zuverlässigste
  Erkennung empfohlen
- 📦 Repo umbenannt zu `DigiWorldExplorer_Android_Bot`, Selbst-Referenzen
  angepasst
- 🧹 Interne Planungsdokumente (`docs/`) aus dem Repo entfernt

### v0.2.1 – 31.07.2026

- 🐞 Krallen-Sammelitem auf dem Spielfeld wird jetzt als Item erkannt statt
  ignoriert (eigener Rotanteil, vorher nur Orange/Pink/Grün)
- 🐞 Dash wird jetzt auch ausgelöst, wenn die Figur in einer Einkesselung
  ohne Fortschritt hin- und herläuft, statt nur bei völligem Stillstand
- 🛡️ Ein wirkungsloser Dash (keine Ladung, Knopf falsch erkannt) blockiert
  die Automatik nach mehreren Fehlversuchen nicht mehr dauerhaft

### v0.2.0 – 31.07.2026

- 🔢 HUD-Zähler (Krallen, Dash) werden erkannt und ausgewertet
- 🔺 Pyramiden werden mit Krallen-Reserve bevorzugt umgangen statt zerschlagen
- 🏃 Mehrere Bewegungsschritte werden bei sicherem Weg gebündelt
- 🐞 Fehlerkennung des Spielers durch Dialogtext behoben
- ⚡ ~1,6× schnellere Bildanalyse
- 🎨 Eigenes App-Icon, Versionsanzeige, Spenden-Link, Signierung

### v0.1.0 – 29.07.2026

- 🧭 Automatische Erkennung des sichtbaren 5×5-Rasters
- 🟠 Priorisierte Sammlung von Items
- 🛑 Sicherheitsstopps bei unsicherem Raster oder Spieler

### Regeln für zukünftige Releases

Bei jeder neuen Version werden gemeinsam aktualisiert:

1. `versionCode`/`versionName` in `app/build.gradle.kts`
2. Changelog in dieser README
3. Git-Tag im Format `vX.Y.Z`
4. GitHub-Release mit demselben Changelog als Release Notes und signierter APK
5. Neu gebaute, signierte APK ohne lokale Keystore-Datei im Repo

## 🔗 Verwandtes Projekt

| Projekt | Plattform | Repo |
| --- | --- | --- |
| DigiWorldExplorer Android Bot (dieses Repo) | Android, nativ, ADB-frei | – |
| DigiWorldExplorer_Bot | Windows + BlueStacks, ADB | [RobinTh0r/DigiWorldExplorer_Bot](https://github.com/RobinTh0r/DigiWorldExplorer_Bot) |

## ☕ Unterstützen

Wenn dir das Projekt gefällt: [paypal.me/thor666](https://paypal.me/thor666)
(auch direkt in der App verlinkt).

## ⚒️ RobinTh0r × Agumon 🦖

✨ Built for the guild · Exclusive for Germon Members ✨

Explore smart. Stop safe. Collect everything.

