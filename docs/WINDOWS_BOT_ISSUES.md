# Zurückgestellte Windows-Bot-Probleme

## ABAB-Pendeln an Hindernissen

Beobachtung vom 29.07.2026: Der Bot lief an einer Hinderniskonstellation wiederholt
links und rechts, obwohl eine sichere Route nach unten in die letzte Rasterzeile
und anschließend nach rechts verfügbar war.

Bekannte Ursache: Der bestehende ABAB-Loop-Guard erkennt die Zustandsfolge
`A -> B -> A -> B`, reduziert aber lediglich die Batchgröße. Die Zielpfadsuche
darf im nächsten Frame weiterhin den unmittelbaren Rückweg auswählen.

Geplanter Fix nach dem Android-M1-Spike:

1. Offline-Regressionstest für die konkrete Sackgassenkonstellation ergänzen.
2. Bei erkanntem ABAB-Pendeln die Rückrichtung als ersten Schritt der nächsten
   Pfadsuche ausschließen.
3. Alternative Route bevorzugen, ohne feste Raster- oder Bildschirmkoordinaten.
4. Syntax- und Offline-Tests ausführen; Live-Test nur nach erneuter Freigabe.
