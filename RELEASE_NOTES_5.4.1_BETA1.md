# DigiWorldExplorer 5.4.1 Beta 1

Experimental automatic-calibration update for device testing. This is **not** a promise that every phone or every Dungeon/Bond issue is fixed.

## Changes

- Use one shared coordinate system for game-image recognition, taps/swipes and grid drawing. Determine Android display/game-window bounds and observed black margins instead of guessing a 9:16 canvas or adding phone-specific profiles.
- Recalibrate after display, density, window, capture-size or rotation changes. Discard old queued DWS coordinates; periodically recheck the visible grid without its own drawn debug grid.
- Handle capture-surface resizing and request full-display sharing on Android 14+. Reject invalid, stale or overlay-covered targets and show a calibration/input-paused status when geometry is unconfirmed.
- Fix small-capture Partner Plus recognition and RGBA image cropping/stride handling.
- Add current calibration geometry, generations and mapped-input/rejection evidence to opt-in diagnostic ZIPs. Keep the existing 50-image limit.
- Keep existing Classic/Bota Walk and experimental All-Sprite movement rules, feature settings and access gates.

## What was actually tested

672 automated tests passed, including software matrices for 1080x2340, 1080x2400 and six other display sizes, simulated navigation/borders/capture scaling/configuration changes, and recorded Partner, Network ad/Matching and Meat Field seed-dialog images. Signed Android build and vital lint passed.

**No live BlueStacks resolution-change test or physical-phone gameplay test of these changes was performed.** Simulated DPI/geometry changes do not prove real Android settings callbacks, game UI reflow or successful in-game actions. Real-device feedback is the purpose of this Beta.

## Please test with diagnostics

Install the APK as an update; no uninstall or data reset is required. Existing app identity/signing certificate and settings are preserved. On physical phones, disable USB debugging before gameplay to avoid game security error `00000038`.

Enable in-app Diagnose before starting the failing automation, stop Diagnose after the failure/run, and export/share the ZIP. Please include the device model, display/zoom/navigation settings and the exact step that failed. Test DWS, Partner expansion/switching/bubbles, Network ad/Matching and Meat Field seed selection. When changing resolution, display zoom or navigation, stop automation first, change settings and restart capture/automation. If geometry cannot be confirmed, the bot may pause instead of guessing a tap.

---

Deutsch: **5.4.1 Beta 1 ist zum Testen der neuen automatischen Kalibrierung gedacht.** 672 automatisierte Tests sind erfolgreich; diese Änderungen wurden noch nicht live auf Handys oder mit umgestelltem BlueStacks geprüft. Bitte Diagnose vor dem Lauf einschalten und danach das ZIP mit Gerät und Fehlerbeschreibung schicken. Auf Handys USB-Debugging während des Spiels ausschalten. Als Update installieren, nicht vorher deinstallieren.
