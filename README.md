# DigiWorldExplorer Android – Machbarkeitsstudie

Dieser Bereich plant eine eigenständige Android-Version des DigiWorldExplorer_Bot. Er ist bewusst vom bestehenden Windows-/ADB-Projekt getrennt, damit APK, Berechtigungen und mobile Bilderkennung unabhängig entwickelt und getestet werden können.

## Zielbild

Eine lokal laufende Android-App, die nach ausdrücklicher Freigabe:

1. den Spielbildschirm über `MediaProjection` erfasst,
2. Raster, Spielfigur, Hindernisse und Energie lokal erkennt,
3. die vorhandene deterministische Entscheidungslogik ausführt,
4. Taps über einen vom Nutzer aktivierten `AccessibilityService` sendet,
5. Status, Pause und Stopp über eine kleine Overlay-Steuerung anbietet.

Eine Modifikation der Spiel-APK ist dafür technisch nicht erforderlich und für dieses Vorhaben ausdrücklich nicht vorgesehen.

## Status

Aktuell: Planung / technische Machbarkeitsstudie. Noch keine funktionsfähige APK.

Siehe [docs/PLAN.md](docs/PLAN.md) für Architektur, Aufwand, PC-Voraussetzungen und Meilensteine.

