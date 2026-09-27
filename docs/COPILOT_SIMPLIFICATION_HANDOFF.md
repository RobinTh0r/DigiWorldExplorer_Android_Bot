# Co-Pilot vereinfachen – konkrete offene Arbeitspakete

Stand: 2026-09-26. Status: Umsetzung mit Benutzer-„go“ begonnen; Nachweise unten beachten.

## Neueste Korrektur des Benutzers – hat Vorrang vor ALLEN älteren Abschnitten unten

- Produktnamen: **Digi Co-Pilot** (Bond/Feld/Rewards/DWS) und **Dungeon Co-Pilot** (manueller Einmallauf).
- Die 20-Minuten-Frist startet DIREKT nach erfolgreich bestätigtem Bond 15/15 inklusive
  Wiederherstellung des ursprünglichen Partners. Feld, Idle-Kiste und optional DWS verbrauchen
  Zeit innerhalb dieser Frist. Ihre Rückkehr darf die Frist NICHT neu setzen oder verlängern.
- Countdown immer als kleines eigenes Overlay-Feld, auch auf Explore/Dungeon/DWS und während
  anderer Schritte; nicht vom Task-Aktionstext abschneiden lassen. Persistenz bleibt erforderlich.
- Digi-Reihenfolge: Bond abschließen/Frist setzen → Feld → Idle-Kiste → optional Explore/DWS
  höchstens 5 Minuten → Home → verbleibende Frist abwarten. Läuft Frist während Arbeit ab,
  erst aktuellen Schritt sicher beenden und Home bestätigen, niemals parallele Bond-Rotation.
- DWS endet vorzeitig bei belegtem Ressourcenmangel. Schritte/Dashes getrennt prüfen:
  null Dashes allein bedeutet nicht automatisch, dass normale Bewegung unmöglich ist.
  Unlesbarer Zähler ist UNKNOWN, kein leerer Vorrat. Nach 5 Minuten keine neuen Bewegungen,
  verifizierter Exit und Home. Bei Verlust der Aufnahme pausieren, kein Exit-Blindklick.
- DWS-Bug untersuchen: `capture/ScreenCaptureService.checkRecognitionTimeouts`,
  `CaptureFrameAnalyzer`, `strategy/AutoMoveController` und DWS-Navigation.
  Eine Ressourcenpause darf nicht pauschal Capture beenden. Echten System-Captureverlust
  weiterhin korrekt melden. Erwartete DWS-/Dungeon-Wartephasen mit begrenztem Task-Watchdog
  behandeln, nicht sämtliche Timeouts entfernen.
- Dungeon Co-Pilot exklusiv: keine unabhängigen Feed/Farm/Summon/DWS/VS-Tower/Login-Taps,
  solange dieser Lauf arbeitet oder geparkt ist. Stop/Pause/Capture-/Foreground-Schutz bleiben.
  Dungeon delegiert eigene Ergebnis-/Fehler-/Network-Schritte ausdrücklich, falls notwendig;
  diese sind Teil desselben Besitzers. Keine globale Blind-Erkennung ausschalten!
- Jeder manuelle Dungeon-Start prüft REGULÄRE Karten neu, selbst wenn heute schon erledigt.
  Session-Versuche pro neuem Start zurücksetzen; tägliche Historie behalten, aber nicht als
  Sperre verwenden. Maximal konfigurierte 3 normale plus tatsächlich verfügbare Ad-Versuche
  pro Pass, sichtbare Restbestände vor jedem Start neu prüfen. Keine automatischen Käufe.
- Nur Apokalymon und VS-Vernichten haben ein hartes persistentes Tageslimit bis 08:00 Berlin.
  Bei erneutem Start diese zwei überspringen, sobald erledigt bzw. unklar bereits ausgelöst.
  Bestehende Daten nicht löschen/auskommentieren; reguläre Completion-Flags von harten Limits trennen.
- Lila Augen für BEIDE aktiven Co-Piloten; Fehler/Stop/Unknown haben weiterhin Vorrang.

Zusätzliche offene Pakete (nach P1, Dungeon-Blocker vorgezogen):
- [ ] D1: exklusiver Dungeon-Besitzer inkl. geparkter Zustand und zentralem Beta-Gate.
- [ ] D2: reguläre Session-Budgets / harte Tageslimits trennen; Resttickets nach erneutem Start.
- [ ] T1: P3-Fristursprung auf Bond-Ende korrigieren; persistent, überall sichtbar, 1-Hz-Ticker.
- [ ] W1: begrenzten DWS-Schritt mit Ressourcenprüfung/Exit in Digi Co-Pilot integrieren.
- [ ] W2: Capture-Timeout-Ursache beim manuellen DWS reproduzieren und passend beheben.
- [ ] UI1: beide Co-Pilot-Namen, lila Augen und übersichtliche Start-/Stop-Aktionen.

Neue Tests: Tagesmaske enthält alle Karten → neuer Pass besucht reguläre Karten dennoch;
Apokalymon/VS bleiben gesperrt; neue Session erhält 3 normale Versuche; Ads werden nicht
durch Neustart künstlich aufgefüllt; Dungeon-Besitzer blockiert alle fremden Probes auch
zwischen Scanframes; Frist läuft während Feld/Rewards/DWS weiter; DWS spätestens nach
5 Minuten beendet und kein neuer Bond-Start vor sicherem Home.
Dieser Plan hat bei Widersprüchen Vorrang vor älteren Modus-/Bond-Ablaufbeschreibungen
im ULTIMATE_AUTOMATION_PLAN.md. Historische Testergebnisse sind kein Nachweis für diese Änderung.

## 0. Auftrag und Arbeitsverzeichnis

Aktives Projekt: `C:\Users\rsc\Documents\DigiWorldExpolorer\.release-v3.2-beta1`.
Version derzeit 5.0.0-beta.1 / Code 67. Bestehende lokale Änderungen gehören zum Stand.
Nicht die Fusion oder einen alten Git-Tag als Ersatz verwenden. Zuerst `git status` ansehen.
Alle unten genannten Kotlin-Pfade liegen unter
`app/src/main/java/de/robinthor/digiworldexplorer/`, Tests entsprechend unter `app/src/test/java/`.

Der Benutzer hat für diesen Auftrag ausschließlich Planung verlangt. Keine App ändern, bauen,
installieren, im Spiel klicken oder veröffentlichen, bis ein späterer Umsetzungsauftrag vorliegt.
Die zurückgezogene GitHub-Beta bleibt zurückgezogen.

## 1. Zielbild – verbindlicher Vorschlag für die spätere Umsetzung

- Den globalen Umschalter Manual / Co-Pilot aus der Hauptseite entfernen.
- Normale Funktionen bleiben einzeln ein-/ausschaltbar. Ein Schalter startet keine Navigation.
- Bond & Friendship bedeutet: auf bestätigtem Home ungefähr alle 10 Sekunden nach einer
  Bond-Blase schauen und eine tatsächlich erkannte Blase einsammeln. Kein Partnerwechsel.
- Im Roboter-Overlay: `Co-Pilot starten` als Beta-Aktion. Das startet einen wiederholten Ablauf:
  Einstieg/Login falls nötig → Home → Bond 15/15 → Meat Field → Home → Idle-Kiste → Home →
  20 Minuten Pause → nächster Durchlauf. Farm ist bei deaktiviertem Farm-Modul explizit übersprungen.
- Ebenfalls im Overlay: `Dungeon-Rotation starten` als separate Beta-Einmalaktion.
  Sie führt genau einen konfigurierten Durchlauf aus und endet wieder auf Home.
- Co-Pilot und Dungeon-Rotation laufen nie gleichzeitig und starten einander nicht automatisch.
- Titel/Touch-to-Start und anschließend auftauchende Idle-Belohnungen sind gemeinsame
  Einstiegshilfe bei eingeschaltetem Bot, unabhängig davon, ob ein Co-Pilot läuft.
- Spätere Erweiterung: Quests. Automatische Ticketkäufe sind noch KEIN Teil dieses ersten Umbaus.

Die Bezeichnung `Co-Pilot` bleibt für den gestarteten Ablauf; keinen zusätzlichen Begriff
`Partner-Bond Rotation Automation` oder dritten Modus einführen.

## 2. Gesicherter Ist-Stand / gemeldete Fehler

- `automation/BondCycleTimer.kt` hält `cooldownUntil` und `awaitingFarm` nur im RAM;
  die Frist basiert auf elapsedRealtime. Sie überlebt keinen Prozess-/Handyneustart.
- Der Countdown beginnt derzeit nach abgeschlossenem Bond und Feld-Rückkehr, nicht beim Start.
  Das ist grundsätzlich richtig. Während der Arbeit soll Fortschritt statt eines erfundenen
  laufenden 20-Minuten-Countdowns erscheinen.
- `feed/BondRotationAnalyzer.kt` startet regulär über Feature-Schalter + FULL_AUTOPILOT;
  `feed/BondRotationRequest.kt` kann Modus und Cooldown einmalig umgehen.
- `automation/BondFarmAnalyzer.kt` verbindet Bond und Feld. Danach fehlt die Idle-Kiste.
- `automation/GameEntryDetector.kt` hat noch eine breite violette Loading-Erkennung.
  Nutzer beobachtet erneut falsches Login/Title während Partner-/Home-Abläufen. Ursache mit
  Frame-Sequenz beweisen, nicht als behoben behaupten oder einfach alle Schwellwerte lockern.
- `automation/ScreenDirector.kt` leitet Screen teils vom FrameOwner ab; BOND bedeutet dort
  Partner/Bond, auch wenn die Handlung auf Home ist. Screen und Task müssen getrennt werden.
- `accessibility/QuickControlOverlay.kt` hängt den Timer an den Aktionstext, der gekürzt wird.
- `automation/BotEyeState.kt` kennt OFF/ACTIVE/SEARCHING/UNKNOWN/ERROR, noch kein Co-Pilot-Lila.
- Die aktuelle Bildschirmaufnahme kann von Android beendet werden. Das ist kein Unknown-Screen:
  Aufnahme neu freigeben lassen, keine erfundenen weiterlaufenden Aktionen anzeigen.
- Frühere komplette 15/15-Läufe sind dokumentiert. Sie beweisen weder persistente Fristen noch
  den neuen Reward-Schritt oder alle Handyformate.

## 3. UI und Zugangsregeln

### Haupt-App

- Behalten: DigiWorld Search, Auto Summon, Bond & Friendship, manueller Meat-Field-Helfer,
  eigenständiger VS/Tower-Helfer sowie Network Defense Ops als verständliche Einzelpunkte.
- Keine Hauptseiten-Schalter für `Bond Rotation` oder `Dungeon rotation` mehr.
  Der bisher zusammengefasste Dungeon-Rotation/VS-Punkt muss aufgeteilt werden: der vorhandene
  manuelle VS/Tower-Helfer darf durch Entfernen der Rotation nicht verloren gehen.
- Eine kompakte Ablauf-Statuskarte: Zustand, Schritt, Fortschritt, nächste Fälligkeit.
- Globale Einstellungen als Untergruppen `Co-Pilot (Beta)` und `Dungeon-Rotation (Beta)`.
  Dort nur Optionen konfigurieren; Änderungen lösen nie einen Start aus.
- Co-Pilot-Optionen zuerst minimal: Feld nach Bond einbeziehen, Idle-Kiste einsammeln.
  Default für neue Konfiguration: beides an, soweit Beta-Zugang vorhanden.
- Dungeon-Optionen: Dungeonauswahl, bis zu 3 normale Versuche + bis zu 2 Ad-Skip-Versuche;
  das sind Obergrenzen, keine Erlaubnis zum Ticketkauf. Aktuelle Bestände prüfen.
  Apokalymon höchstens einmal pro Spieltag; VS-Vernichten separat einmal pro Spieltag,
  nur bei erfüllten Voraussetzungen, danach vorhandener VS-Helfer separat nutzbar.
- Globaler Ad-Skip-Pass bleibt gemeinsame Einstellung. Network Defense Ops bleibt frei.
- Bestehende Beta-Gates des manuellen Meat Field/VS-Helfers nicht versehentlich entfernen.

### Overlay

| Zustand | Hauptaktion | Weitere Aktion |
| --- | --- | --- |
| Kein Beta-Zugang | Co-Pilot/Dungeon gesperrt mit Beta-Hinweis | normale Helfer weiterhin nutzbar |
| Aufnahme aus | Bot starten / Aufnahme freigeben | kein automatischer Ablaufstart |
| Kein Ablauf aktiv | Co-Pilot starten; Dungeon-Rotation starten | Co-Pilot neu beginnen … |
| Co-Pilot arbeitet | Co-Pilot pausieren | Stop jederzeit; Dungeon-Start gesperrt |
| Co-Pilot in Pause | Co-Pilot pausieren, Restzeit sichtbar | Co-Pilot neu beginnen … |
| Co-Pilot manuell pausiert | Co-Pilot fortsetzen | Co-Pilot neu beginnen … |
| Dungeon aktiv | Dungeon stoppen | Co-Pilot-Start gesperrt |

- `Co-Pilot neu beginnen …` braucht eine kurze Bestätigung: setzt nur Co-Pilot-Fortschritt und
  die Bond-Pause zurück, kann bereits besuchte Partner erneut besuchen. Kein Tageslimit/Ad-Zähler
  wird gelöscht. Einen vorhandenen Action-Callback vorher ungültig machen; erst dann neu starten.
- Normaler Start/Fortsetzen umgeht niemals eine gültige Frist. Alten Force-Bond-Knopf ersetzen,
  nicht beide Einstiegspunkte dauerhaft anbieten.
- Beta-Gate in UI UND zentralem Start-/Dispatch-Pfad prüfen, über vorhandenen
  `license/SupporterLicenseManager.kt`; alte Preferences oder Requests umgehen die Prüfung nicht.
- Kein Beta-Zugang: Optionen sichtbar, aber deaktiviert. Kein heimliches Aktivieren durch Migration.

## 4. Ablaufmodell und Zustandsübergänge

Neue reine Kotlin-Steuerung vorschlagen: `automation/CopilotController.kt` mit
`CopilotPhase`: STOPPED, ENTRY, WAIT_HOME, BOND, FIELD, REWARDS, RETURN_HOME, COOLDOWN,
PAUSED_USER, PAUSED_CAPTURE, PARKED_ERROR, RECOVERY_REQUIRED.
Ein gemeinsamer Laufbesitzer: NONE / COPILOT / DUNGEON. Nicht noch eine zweite unabhängige
Action-Pipeline zusätzlich zu FrameOrchestrator anlegen.

1. Start: Beta, Aufnahme, Accessibility und Laufbesitzer prüfen; gültigen gespeicherten Zustand laden.
2. Falls Cooldown vorhanden: bis Fälligkeit warten. Nicht bereits jetzt BondRotationRequest.start().
3. Bekannte Login-/Idle-Einstiegsschritte abarbeiten; danach bestätigtes Home abwarten.
4. BOND: 15 bestätigte Wechsel, zuerst ursprünglichen Partner speichern, diesen zuletzt wiederherstellen.
   Home-Bubble-Collector zwischen den Wechseln verwenden. Nicht zweimal parallel auf die Blase tippen.
5. FIELD: genau ein Besuch, wenn konfiguriert; ansonsten explizites SKIPPED_DISABLED-Ergebnis.
   Ein Feld mit nichts Erntereifem/ohne Samen ist ein normaler Abschluss, kein Fehler.
6. REWARDS: die Idle-Kiste links unten öffnen, verifizierte Belohnungen nehmen und Dialoge schließen.
7. RETURN_HOME: frische Home-Evidenz verlangen. Erst dann den ganzen Zyklus erfolgreich abschließen.
8. COOLDOWN: exakt eine Frist jetzt + 20 Minuten speichern. Wiederholte Frames dürfen sie nicht
   immer neu setzen. Fälligkeit allein erzeugt keinen Tap; Home/Entry müssen erneut geprüft werden.
9. Neuer Durchlauf erst, wenn Fälligkeit erreicht UND Co-Pilot ausdrücklich aktiv UND keine
   Unterbrechung vorhanden. Normaler Bond-Helfer darf während Home-Wartezeit weiter Blasen sammeln.

Pause/Stop: ausstehende Gesten/Callbacks über runId invalidieren, keinen neuen Tap senden.
Frist und bestätigten Fortschritt behalten. Stop startet später nichts von allein.
Nach Prozessneustart oder Capture-Verlust: Zustand anzeigen und ausdrückliches Fortsetzen verlangen.
Eine laufende Rotation nie blind bei CONFIRM fortsetzen. Letzte bestätigte Partner-ID und
ursprünglichen Partner aus neuem Grid abgleichen; bei unklarer Identität RECOVERY_REQUIRED.
Kein erzwungenes Zurückwechseln während Stop oder ohne bestätigte Screens.
Dungeon-Einmallauf kehrt zu Home zurück und endet; er reaktiviert keinen pausierten Co-Pilot.

## 5. Persistenter globaler Timer

Neue kleine Ablage `automation/CopilotStore.kt` (versionierte Preferences genügt), mit reinem
Clock-/Store-Interface für Tests. Einen Autoritätsort schaffen; BondCycleTimer wird Adapter oder
wird nach Migration entfernt, darf nicht zusätzlich seine eigene unabhängige Frist führen.

Mindestens speichern: schemaVersion, runId, phase/checkpoint, originalPartnerId,
bestätigte besuchte Partner, bondCompleted, farmOutcome, rewardsOutcome,
cycleCompletedAtEpochMillis, nextEligibleAtEpochMillis, letzte bekannte verbleibende Dauer,
letzte Wall-/Monotonic-Zeit zur Plausibilitätsprüfung. Atomar zusammengehörige Felder schreiben.
UI-Refresh schreibt nicht jede Sekunde Preferences; nur Zustandswechsel/Checkpoint speichern.

- Dauer bleibt 20 Minuten; in einem Prozess monotone Uhr für verstrichene Dauer verwenden.
- Über App-/Handyneustart: UTC-Epoch-Zeitstempel speichern. Lokal formatiert als z.B. `weiter ab 14:32`.
  Keine lokale HH:mm-Zeichenkette speichern: sonst brechen Mitternacht, Zeitzone und Sommerzeit.
- Beispiel: fertig 14:00, App um 14:05 schließen, um 14:12 öffnen → Rest 08:00.
  Öffnen nach 14:20 → `Bereit`, aber erst nach Fortsetzen tatsächliche Aktionen.
- Uhrzeit manuell verstellt/Frist unplausibel: RECOVERY_REQUIRED, keine negative Anzeige oder
  endlose Frist und keine sofortige doppelte Rotation. Explizites Neu-beginnen bleibt verfügbar.
- Fälligkeit gehört nicht zum täglichen Reset! Dungeon-/Ad-Zähler weiterhin 08:00 Europe/Berlin
  über `DailyResetClock`, `EntryDailyStore`, `DungeonDailyStore`, unabhängig von Telefon-Zeitzone.
- Beim erstmaligen Upgrade fehlt eine persistierte Frist: keine abgeschlossene Rotation erfinden;
  in STOPPED/Bereit starten und Benutzerstart abwarten.

## 6. Timeranzeige und Augen

`DirectorSnapshot` erweitern: tatsächlicher screen, task, phase, runOwner, progress,
nextEligibleAt, remainingMillis, pauseReason. Keine Erkennung aus Status-Texten ableiten.
Auch Haupt-App und Overlay lesen denselben Snapshot/Store.

- Zeile 1 z.B. `Auto Mode Active · Home`.
- Zeile 2 während Arbeit: `Bond 7/15`, `Feld prüfen` oder `Belohnungen abholen`.
- Zeile 2 im Cooldown: `Nächster Lauf 18:42`; Datum/Uhrzeit zusätzlich in Haupt-App/Detailkarte.
- Timer als eigenes Feld rendern, nicht an einen abgeschnittenen Aktionstext anhängen.
- Während BOND noch kein Pause-Countdown: optional `Pause danach: 20 min`. Das erklärt,
  weshalb ein globaler Timer erst nach vollständigem Zyklus zählt.
- UI-Ticker ungefähr 1 Hz auch bei statischem Bild; unabhängig vom Eingang neuer Capture-Frames.
  Ticker sauber mit Overlay-/Activity-Lifecycle starten/stoppen, keine WakeLocks nur für Anzeige.
- Augen lila bei aktivem Co-Pilot, auch im regulären Cooldown; Hinweis `Co-Pilot steuert –
  bitte nicht gleichzeitig bedienen`. Pause/Stop jederzeit erreichbar.
- Fehler rot, Capture aus/Aus geschlossene graue Augen, dauerhaft Unknown grau, Suche gelb
  haben Vorrang vor Lila. Normale Helfer grün. Animationen weiterverwenden, kein neues Bildasset nötig.
- Bei Co-Pilot-Pause kein `Auto Mode Active`. Eine normale Wartezeit ist kein Fehler.

## 7. Idle-Kiste nach dem Feld (jetzt konkret statt „Bond-Belohnungen“)

Gemeint ist die Kiste links auf HOME, unter den gelben Beschleunigungspfeilen/x1 und über
der unteren Navigationsleiste. Nicht Missionen rechts und nicht Partner-/Bond-Belohnungsmenü.

1. Reale Frames für volle/bouncende Kiste, leere Kiste, geöffnete Idle-Ansicht, Erhalten,
   Ergebnis und abgeschlossene Ansicht aufnehmen. Aktuelles Home muss positiv bestätigt sein.
2. Position aus Home-Viewport und stabiler Nachbarschaft ableiten. Animationstolerante ROI,
   mehrere Frames; genau einen Tap innerhalb verifizierter Kistenfläche, kein blindes Rasterklicken.
3. Neuen `HomeIdleRewardDetector` und kleinen `HomeIdleRewardController` vorsehen. Für den
   Dialog die vorhandenen `IdleRewardDetector`, `GameEntryController`, `GameEntryAnalyzer`
   wiederverwenden/refaktorieren, nicht parallel zweite Claim-Taps abfeuern.
4. Claim nur bei bestätigtem aktivem `Erhalten`; Ergebnis schließen und erneut lesen.
   Optionaler Ad-Skip nur mit globalem Pass, aktuellem positivem Restzähler und Tagesfreigabe.
5. `EntryDailyStore` verhindert erneute tägliche Ad-Prüfung nach belegter Erschöpfung bis 08:00.
   Normale Idle-Belohnungen dürfen bei jedem Zyklus neu beansprucht werden.
   Gestenerfolg oder die bloße Zahl versuchter Klicks beweisen keinen erfolgreichen Ad-Claim.
6. Bei bestätigter leerer Ansicht schließen und als NOTHING_AVAILABLE abschließen.
   Nicht erkannte Kiste/Ansicht ist UNKNOWN und NICHT erfolgreich/leer.
7. Dialog über verifizierten äußeren Bereich/Close schließen, Home bestätigen, dann Zyklusfrist setzen.
   Pro Übergang Timeout und begrenzte Wiederholung, keine Dauerklicks bei derselben Ergebnisblase.

## 8. Erkennung und Handyformate vor Feature-Ausbau stabilisieren

- Dateien: `GameEntryDetector`, `HomeScreenDetector`, `PassiveScreenClassifier`, `ScreenDirector`,
  `GameEntryAnalyzer`, `capture/ScreenCaptureService`, `feed/PartnerGridDetector`.
- Gelber Home-Pin plus rechte Icons/Home-Steuerelemente sind Home-Evidenz, keine Stage-Farbe.
- LOGIN_LOADING braucht spezifische Titelmerkmale zusätzlich zur Farbe. GAME_ENTRY darf eine
  aktive Partnerseite nicht allein wegen lila Bildteilen übernehmen.
- Tatsächlicher Screen, erwarteter Zielscreen und Besitzer sind getrennte Dinge:
  `Task=Bond, Screen=Home` ist korrekt. Besitzer BOND darf nicht pauschal Partner/Login anzeigen.
- Ein gemeinsamer stabiler Klassifikationsentscheid pro ausgewertetem Frame; gleiche Evidenz
  für Anzeige und Action-Gate. Letzten Screen kurz anzeigen ist erlaubt, aber kein alter Screen
  darf einen neuen Tap bei aktuell unbekanntem Bild autorisieren.
- Sequenztests: Home → Partner → Raise → Home und Home → Feld → Kiste → Home; keine falschen
  Login-Taps oder konkurrierenden Besitzer. Animationen/Overlay-Abdeckung/Popups einschließen.
- Viewport bei Erkennung UND Klick identisch benutzen. Alle Schritte, nicht nur Home, für
  16:9/18:9/19,5:9/20:9, echte Inset-/Navigationsleisten und Ausschnitte prüfen.
  Zentriertes 9:16-Fitting ist nur ein Kandidat, keine Garantie für jedes Handy.
- Skalierte Fixtures nicht als echte Geräteprüfung verkaufen. Mindestens ein reales langes
  Handyformat mit Home, Partner, Feld und Idle-Dialog nachtesten; offene Geräteprüfung klar nennen.

## 9. Kleine Arbeitspakete – genau in dieser Reihenfolge

Jedes Paket einzeln umsetzen und prüfen. Kein Komplett-Neuschreiben funktionierender Module.

- [ ] P1: Fehlklassifikationen reproduzieren und beheben (Abschnitt 8). Neue echte Sequenzfixtures,
  Negativtests; erst danach weitere Aufgaben in die Pipeline hängen.
- [ ] P2: Reinen CopilotController/RunOwner und Testfälle für Abschnitt 4 schreiben.
  Bestehende BondRotation/FarmController als Unteraufgaben verwenden, keine zweite Rotation bauen.
- [ ] P3: CopilotStore + Uhr-/Timer-Migration (Abschnitt 5). Tests ohne 20 Minuten Echtzeit-Wartezeit.
- [ ] P4: Idle-Kiste/Claim-Handoff (Abschnitt 7), einschließlich Ad-Tageszustand und leeren Ergebnissen.
- [ ] P5: Capture-Pipeline und Requests an zentralen Controller anschließen. Relevante Dateien:
  `feed/BondRotationAnalyzer.kt`, `BondRotationRequest.kt`, `automation/BondFarmAnalyzer.kt`,
  `BondFarmCycle.kt`, `BondCycleTimer.kt`, `GameEntryAnalyzer.kt`, `capture/ScreenCaptureService.kt`,
  `strategy/AutomationState.kt`, `dungeon/DungeonRotationRequest` (Dateipfad per rg bestimmen).
  Normale Feed-Prüfung ca. 10 s; während Rotation expliziter Sammelschritt darf sofort prüfen.
- [ ] P6: Haupt-App und Overlay vereinfachen (Abschnitt 3): `MainActivity.kt`,
  `CaptureConsentActivity.kt`, `accessibility/QuickControlOverlay.kt`, DE/EN `strings.xml`,
  `dungeon/DungeonSettings.kt`. Alle Startwege (inkl. App-/Capture-Neustart) laden denselben Store.
- [ ] P7: Snapshot, dauerhafte Anzeige, lila Augen: `automation/ScreenDirector.kt`,
  `BotEyeState.kt`, `accessibility/BotEyeStatusView.kt`, Overlay und Haupt-App.
- [ ] P8: Alte Modusabhängigkeiten entfernen/migrieren. `automation_mode` nicht mehr als Gate
  für einzelne Helfer benutzen. Alte FULL_AUTOPILOT-/auto_bond_rotation-Werte dürfen nach Update
  keine Navigation starten. Bestehende Optionen erhalten, neuer Ablauf startet zunächst gestoppt.
  `auto_dungeon` vom eigenständigen VS/Tower-Helfer unterscheiden; Tagesledger nicht löschen.
  `support/SupportExportButton.kt` und Hilfetexte dürfen keine alten Modusnamen mehr exportieren.
- [ ] P9: Integration/Abnahme, Plan und Release-Texte aktualisieren. Veröffentlichung separat.

## 10. Abnahme – nichts ohne passenden Nachweis abhaken

- [ ] Helfer ohne Co-Pilot: mindestens 30 s Home; Blasenprüfung ungefähr alle 10 s,
  keine Partner-/Feldnavigation. Blase wird nur nach Erkennung getippt.
- [ ] Mit/ohne Beta-Zugang: UI gesperrt und direkt gesetzter Request ebenfalls ohne Navigation.
- [ ] Ganzer Zyklus: 15 bestätigte Partner, ursprünglicher zuletzt, Feld einmal,
  volle ODER nachweislich leere Idle-Kiste, Home, Frist einmal gesetzt.
- [ ] Feld deaktiviert: explizit übersprungen, Rewards/Home/Cooldown trotzdem erreichbar.
- [ ] App schließen, Prozess töten und Handy-Neustart simulieren: Frist bleibt, keine ungewollten
  Aktionen. Fortsetzen während Pause wartet; Fortsetzen nach Ablauf startet erst bei erkanntem Screen.
- [ ] Stop/Captureverlust während Raise/Claim/Ad: keine nachträglichen Callback-Taps;
  sicherer Checkpoint oder Recovery-Hinweis, kein erfundener Abschluss.
- [ ] Reset-Co-Pilot: Frist darf weg, tägliche Dungeon-/Ad-Limits bleiben identisch.
- [ ] Frist über Mitternacht, Zeitzonenwechsel, Sommerzeit und manuelle Uhrkorrektur prüfen.
- [ ] Overlay 1-Hz-Timer trotz statischem Bild; Fortschritt sichtbar, lila Augen/Fehlerprioritäten korrekt.
- [ ] Dungeon ein Durchlauf, Home-Abschluss, kein paralleler Co-Pilot, keine automatischen Käufe.
- [ ] Keine Home/Partner/Login-Verwechslung in aufgezeichneten Übergangssequenzen.
- [ ] Tests + signierter Build; danach echte Live-Belege dokumentieren, einschließlich Grenzen.

Bewährter Build aus dem aktiven Projektverzeichnis (PowerShell):

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' -classpath '.gradle-codex-build/tools/gradle-9.4.1/lib/*' org.gradle.launcher.GradleMain '-Pandroid.aapt2FromMavenOverride=C:\Users\rsc\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe' testDebugUnitTest assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`. Bestehende Signatur/Paket/Version erhalten.
Bei später autorisiertem Live-Test per ADB installieren mit `install -r`, nie App-Daten löschen.
Kein `uiautomator dump` während Automation; es kann Accessibility stören. Screenshots und Logs nutzen.

## 11. Später, ausdrücklich noch offen

- [ ] Missionen/Quests rechts: eigene Erkennung, freie Belohnungen, Ergebnisdialoge, Home-Rückkehr.
- [ ] Optionale Ticketbeschaffung: zuerst Preise/Währungen/Kauflimits und ausdrückliche Auswahl
  spezifizieren. Unbekannter Kaufdialog bleibt gesperrt. Nicht aus „3 Versuche“ Kaufrecht ableiten.
- [ ] Tagesübersicht mit erledigt/offen/übersprungen/Fehler und Reset 08:00 Europe/Berlin.
- [ ] Weitere Aufgaben in Co-Pilot aufnehmen erst nach einzeln verifiziertem Ablauf.
- [ ] Gekkomon bleibt verborgen/letzte Priorität.

## 12. Übergabe an ein kleineres Modell

Arbeitsanweisung für den nächsten Umsetzungsauftrag:
„Lies diese Datei. Führe das erste offene Paket aus, nicht alle auf einmal. Prüfe Ist-Code und
git diff, erhalte vorhandene Änderungen. Implementiere die expliziten Übergänge und Tests dieses
Pakets. Trage Dateien, Testergebnis, Live-Beleg und verbleibende Einschränkungen direkt darunter
ein. Hake nur erfüllte Punkte ab. Stoppe nicht nach einem Plan, wenn Umsetzung beauftragt wurde;
erfinde aber keine live getesteten Ergebnisse. Keine Veröffentlichung ohne gesonderten Auftrag.“

### Fortschrittsprotokoll 26.09.2026 (keine vollständige Live-Abnahme)

- P1 umgesetzt: konkrete Title-/Touch-Start-Textmuster statt pinkem Hintergrund;
  Partnerseiten geben den Entry-Owner frei. Screenshot-Replays einschließlich skalierter
  und zentrierter Formate bestehen; dies ersetzt keinen Test auf echten Handys.
- D1 umgesetzt: `DungeonRotationRequest.ownsFrames()` reserviert auch Zwischen-/Parkframes
  in `ScreenCaptureService`. Nur eigene Resultate, Niederlagen und der explizit ausgewählte
  Network-Defense-Kampf werden delegiert. Beta-Prüfung zusätzlich am Request und Analyzer.
- D2 umgesetzt: `DungeonPassPolicy`/`DungeonPassUsage` trennen aktuelle Versuche von Tageshistorie.
  Reguläre Karten werden bei neuem manuellem Start erneut geprüft. Nur Apocalymon und Daily/VS
  bleiben hart gesperrt; reservierter Versuch zählt vorsichtshalber auch bei unklarem Ergebnis.
  Tägliche Ad-Zähler und 08:00-Europe/Berlin-Reset bleiben erhalten. Keine Ticketkäufe implementiert.
- T1 teilweise umgesetzt: Frist direkt nach Bond, Feld verlängert sie nicht; eigener 1-Hz-Text
  im Overlay. `bond_cycle_clock/due_epoch_ms` überlebt Prozess-/Geräteneustart und wird auf eine
  monotone Restzeit abgebildet. Rückwärtskorrekturen sind auf 20 Minuten begrenzt.
  Noch KEIN vollständiger persistenter Co-Pilot-Checkpoint mit Recovery/Fortsetzen!
- UI1 teilweise: Dungeon-Start und aktive Anzeige heißen Dungeon Co-Pilot, Bond-Anzeige Digi
  Co-Pilot; aktive bekannte Screens lila Augen, Fehler/Unknown/Off behalten Vorrang.
  Die Haupt-App und alte Modusschalter sind noch nicht vollständig migriert.
- Signierter Release-Build erfolgreich; 225 Unit-Tests, 0 Fehler. Neue Tests für
  Pass-Resets/Tagessperren und Bond-Frist. `git diff --check` ohne Whitespace-Fehler.
  Letzte Live-Sichtung: Spiel auf Beschwörungs-Belohnungen, kein verifizierter kompletter
  Dungeon-Lauf dieses neuen Builds. APK nicht veröffentlicht.

### Nächste Schritte (weiter offen, nicht als erledigt behandeln)

1. D1/D2 live abnehmen: reguläre Resttickets bei erneutem Start, Apocalymon/VS gesperrt,
   Network-Defense und Belohnungen ohne konkurrierenden VS-Owner, bestätigte Home-Rückkehr.
2. P2–P5 zentralen Digi-Co-Pilot-Controller/Store fertigstellen, einschließlich explizitem
   Start/Stop/Recovery und keinen automatischen Navigationen nach bloßem App-Neustart.
3. Nach Feld die Home-Kiste gezielt öffnen, vorhandenen Idle-Controller delegieren,
   Bestätigung/Ad-Tageslimit und verifizierte Rückkehr abwarten. Nicht aus einem Tap Erfolg ableiten.
4. Optionalen DWS-Abschnitt maximal fünf Minuten anfügen; ExploreMenuDetector liefert bereits
   worldSearchTarget. Ressourcen separat bewerten; Dash=0 bedeutet nicht Schritte=0.
   Nach Ende/leer Home bestätigen, Restzeit bis ursprünglichem Bond-Termin warten.
5. W2 Ursache eingegrenzt: AutoMoveController schaltet bei erfolglosen Aktionen global aus
   und ruft ScreenCaptureService.stopForStuck auf. In einem eigenen DWS-Co-Pilot-Zustand
   stattdessen Bewegung stoppen und sichere Rückkehr anfordern; Capture weiterlaufen lassen.
   Der allgemeine Capture-/Unknown-Watchdog darf NICHT pauschal entfernt werden.
6. P6–P9 UI-Migration, persistente Recovery-Tests und gesamte Live-Abnahme nachholen.

### Fortschritt 26.09.2026 – Folgepaket

- P2 reine Steuerung vorhanden: `CopilotController`, explizite Phasen/Kommandos und `RunOwner`;
  Tests decken Fristbeginn bei Bond, optionale Unteraufgaben, Home-Grenze und Capture-Pause ab.
  Die bestehenden Laufzeitanalysatoren sind noch nicht vollständig auf diesen Controller migriert.
- P3 Grundablage vorhanden: `CopilotStore` versioniert Zustände und wandelt nach Prozessabbruch
  aktive Gesture-Phasen in `RECOVERY_REQUIRED` um. Noch nicht Autorität aller Runtime-Module;
  `BondCycleTimer` bleibt vorläufig Fristquelle.
- P4 technisch angeschlossen: `HomeIdleRewardDetector/Analyzer` erkennt nach frisch bestätigtem
  Home die vom Nutzer bestätigte hellblaue Kiste mit grün-blauem Stapel, wartet zwei Frames und
  delegiert Dialog/Claim/Ad an `GameEntryAnalyzer`. Stabile Abwesenheit ergibt nichts verfügbar.
  Negativfixtures und ein aktueller 720x1280-Liveframe vorhanden; tatsächlicher Claim noch live testen.
- W1 erster begrenzter Ablauf: optionaler Overlay-Schalter `DWS im Digi Co-Pilot (5 min)`,
  Home → Explore → DWS, maximal fünf Minuten, sichere Rückkehr. Festfahren beendet im Co-Pilot
  nur DWS statt Capture/gesamter Automation. Ressourcenzähler gelten weiterhin nicht blind als leer.
  Navigation und Ressourcenende noch live testen.
- Alte FULL_AUTOPILOT-Kopplung aus Bond/Farm und Anzeige entfernt. `DigiCopilotRequest` ist
  pro Prozess nur durch den Overlay-Knopf aktivierbar; Einstellungen allein starten keine Navigation.
  Dungeon-Start und globaler Stop beenden den Digi Co-Pilot. Haupt-App-Modusauswahl noch entfernen.

### Live-Debug Bond/Start 26.09.2026 (aktueller Stand)

- BlueStacks komplett neu gestartet und echter Spielstart bis Home geprüft. Lange Login-/Update-
  Ketten dürfen MediaProjection nicht mehr durch den allgemeinen 60-s-Unknown-Timeout beenden,
  solange das Spiel im Vordergrund ist. Außerhalb des Spiels bleibt der Timeout bestehen.
- Während `DigiCopilotRequest` aktiv ist, dürfen generische Dungeon-, Network-, Gekkomon- und
  Summon-Erkenner die Bond-Frames nicht mehr übernehmen. Der Farm-Aktionsanalysator wird erst beim
  expliziten Bond→Farm-Handoff freigegeben. Live-Beleg: OPEN→SELECT→RAISE→CONFIRM→HOME→COLLECT.
- COLLECT ist für schwierige/verlorene Stages auf 90 s (+15 s harte Frist) erweitert. Die Rotation
  bleibt durch kurz verschwindende Home-/Bubble-Frames aktiv und Capture läuft weiter.
- Im von der Rotation bereits bestätigten Home/COLLECT-Fenster werden wechselnde cyanfarbene
  Stage-Hintergründe nicht erneut als Sicherheitsmerkmal verlangt. Der stark animierte Bubble-
  Mittelpunkt darf springen; ein positiver Bubble-Treffer autorisiert dort den Klick. Außerhalb
  dieses eng begrenzten Fensters bleiben die bisherigen Home- und Vier-Frame-Prüfungen bestehen.
- Unit-Tests und signierter Release-Build erfolgreich. APK lokal gebaut, nicht veröffentlicht.
- Noch offen für die nächste Live-Abnahme: Logbeleg `collect bubble=...` und danach `step=OPEN`
  für Partner 2 mit dem allerletzten Ein-Treffer-Build sowie vollständige 15er-Rotation.
- Beim frischen Start erschienen mehrere neue Kollaborations-Popups. Der bestehende Entry-Flow
  hat diese konkrete Kartenserie nicht selbst geschlossen; dafür separates Fixture/Handling ergänzen.

### Abnahme ohne Dungeon 26.09.2026 (installierter Release-Stand)

- Dungeon Rotation wurde auf ausdrücklichen Wunsch **nicht** gestartet. Apocalymon/VS und deren
  Tagesstatus wurden nicht verändert.
- Bond Rotation live vollständig durchgelaufen: `visited=15`, ursprünglicher Partner wiederhergestellt
  (`restored=10`) und mehrere echte `collect bubble=...`-Aktionen im Log bestätigt. Bei verlorenen
  Stages bleibt COLLECT über kurz verschwindende Bubbles aktiv; fehlende Bubbles führen nach dem
  begrenzten Fenster sicher zum nächsten Partner. Die eigenständige Bond-&-Friendship-Funktion nutzt
  denselben live bestätigten Bubble-Analyzer, wurde aber nicht als separater Schalterlauf wiederholt.
- Digi-Co-Pilot-Handoff live bis Home → Explore → Meat Field bestätigt. Eine falsche Übernahme des
  Feldes durch `GameEntryAnalyzer` wurde behoben: positiv erkanntes Meat Field schließt Entry-Rewards
  nun vor jeder Dialogaktion aus. Der komplette Rückweg Feld → Home → Kiste/Idle-Rewards wurde nach
  dem letzten APK-Install noch nicht erneut Ende-zu-Ende ausgeführt.
- Meat Field mit reifen/blanken Beeten live geprüft: ernten, Samenpriorität, alle verfügbaren Beete
  bepflanzen und Gießkannen bis 0 ausführen. Im final installierten Build wird das Feld sofort als
  `Active · Meat Field` erkannt, `Farm: nothing ready` bleibt ohne Fehltaps ruhig und der persistente
  Bond-Timer zählt im Overlay weiter.
- Beta-Gates geprüft und vervollständigt: Dungeon, Bond Rotation, Meat Field, Co-Pilot-DWS sowie beide
  Overlay-Co-Pilot-Starts benötigen eine Supporter/Beta-Lizenz. Der alte FULL_AUTOPILOT-Modus-Chip
  öffnet ohne Lizenz den Freischaltdialog; ein gespeicherter Altwert fällt bei Start/Resume auf Manual
  zurück. Network Defense, Auto Summon und Bond & Friendship bleiben reguläre Funktionen.
- `testDebugUnitTest assembleRelease`: BUILD SUCCESSFUL, 77 Gradle-Tasks, keine Testfehler. Signierte
  APK per `install -r` erfolgreich auf BlueStacks installiert und Capture danach neu gestartet.

### Abschlussstand UI und Digi-Co-Pilot 26.09.2026

- Die Hauptseite enthält keine Manual-/Co-Pilot-Moduswahl und keinen kombinierten
  Dungeon-Rotation-&-VS-Schalter mehr. Die regulären Funktionen bleiben oben unverändert.
- Alle neuen Funktionen sind am Ende in einem kompakten gelben Block `BETA AUTOMATION` gebündelt:
  Digi Co-Pilot, Dungeon Co-Pilot, Bond Rotation und Meat Field. Jede Zeile hat einen eigenen
  Konfigurationsknopf und eine Kurzhilfe; gestartet und gestoppt wird ausschließlich im Overlay.
- Overlay-Bedienung: kurzer Druck auf den runden Bot öffnet das Aktionsmenü, zwei Sekunden halten
  blendet die seitliche Statusanzeige ein oder aus. Derselbe Schalter ist zusätzlich im Menü.
  Die Start-/Stop-Aktionen für beide Co-Piloten bleiben zentral durch den Beta-Key gesperrt.
- Bond-Rotation live 15/15 abgeschlossen und Ausgangspartner wiederhergestellt. Danach Feld,
  Home-Kiste/Idle-Belohnung und Rückkehr zu Home live erfolgreich durchlaufen.
- DWS-Navigation und echte Bewegung wurden live bestätigt. Das bisherige Android-Back am Ende
  landete falsch; der aktuelle Build benutzt deshalb das feste In-Game-X und anschließend die
  bekannte Home-Navigation. Dieser neue Rückweg ist gebaut und durch Tests abgesichert, aber noch
  nicht erneut in einem vollständigen fünfminütigen Live-Lauf abgenommen.
- Dungeon-Co-Pilot bleibt bewusst von dieser Abnahme ausgenommen. Vor Veröffentlichung fehlen daher
  noch ein kompletter Dungeon-Tageslauf und ein vollständiger DWS-Rücklauf mit dem aktuellen APK.

### Beta-2-Entscheidungen 26.09.2026

- Apocalymon ist für Beta 2 unabhängig von alten gespeicherten Einstellungen standardmäßig aus und
  in den Dungeon-Einstellungen grau gesperrt. Ursache: Fenster wird erkannt/geöffnet, der Start-Tap
  ist live noch nicht zuverlässig. Vor Reaktivierung Fixture und bestätigten Zustandswechsel ergänzen.
- DemiDevimon und Bakemon erhalten maximal 3 normale plus 2 Ad-Skip-Versuche (= 5). Danach geht die
  Rotation weiter. Ein optionaler konfigurierbarer Endlos-Loop nach Vorbild des Classic-Tower-Loops
  bleibt offener Punkt für eine spätere Beta und darf nicht stillschweigend Standard werden.
- Während Dungeon Co-Pilot aktiv ist, wird der eigenständige Network-Defense-Schalter zur Laufzeit
  suspendiert und nach Abschluss/Stop wiederhergestellt. Die von der Rotation explizit delegierte
  Diaboromon-Strategie bleibt aktiv; es laufen keine zwei konkurrierenden Besitzer.
- Classic VS/Tower Loop ist wieder als eigener Punkt in der Haupt-App sichtbar. Overlay-Aktionen und
  Optionen der Next-Gen-Module tragen eine gelbe BETA-Markierung.
- Nach Live-Rückmeldung: Partner-COLLECT scannt ab Home sofort für 15 Sekunden engmaschig (jeder
  Capture-Frame, intern maximal alle 100 ms), tippt ausschließlich bei positiver Bubble-Erkennung
  und wartet nach bestätigtem Tap eine Sekunde. Danach bleibt ein langsameres Sicherheitsfenster bis
  30 Sekunden. Digi Co-Pilot respektiert die im Overlay aktivierten Module und schaltet Bond/Meat
  nicht mehr selbst ein. Ein weiterhin sichtbarer Reward-Result-/Erhalten-Bildschirm erhält bis zu
  drei erneute, zeitlich begrenzte Taps statt nach einem verschluckten Tap bis zum Timeout zu warten.
- Apocalymon-Ursache gefunden: Sein Einzelknopf liegt mittig bei ca. x=.50/y=.756 und der Material-
  zähler bei y=.708; der gemeinsame Detector erwartete die normale Zwei-Knopf-Geometrie x=.66/y=.70.
  Eigene Geometrie ist implementiert, die Beta-2-Sperre bleibt bis zu einem Live-Test bestehen.
- Diaboromon-Abbruch war trotz deaktiviertem Classic-Schalter fest im delegierten Network-Analyzer:
  `rotationOwned=true` umging zwar den Feature-Schalter, erbte aber weiterhin den Aufgeben-Tap.
  Jetzt gibt nur der Classic Network-Defense-Loop auf; Dungeon Co-Pilot wartet durch Endboss und
  Kampf bis zur bestätigten Rückkehr. Ein Ownership-Regressionstest hält diese Trennung fest.
- Overlay hat nun auch `Home-Belohnungen` als eigenen Beta-Schalter. Digi Co-Pilot startet nur die
  ausgewählten Module Bond, Meat Field, Home-Belohnungen und DWS; bei leerer Auswahl startet er nicht.
- Overlay-Layout live geprüft: Der Digi-Co-Pilot-Start/Stop-Button und genau diese vier Module stehen
  gemeinsam in einem gelb umrandeten Beta-Rahmen. Auto Summon, Bond & Friendship und Network Defense
  bleiben als normale Einzelfunktionen außerhalb; Dungeon Co-Pilot bleibt eine getrennte Beta-Aktion.
