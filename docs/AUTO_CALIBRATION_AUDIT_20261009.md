# Automatic calibration audit — 9 October 2026

Status: implemented after the published 5.3.0 release and subsequently published as **5.4.1 Beta 1** at the user's explicit request. Not installed or live-tested. This is an engineering verification, not certification of all Android devices.

Publication follow-up: the user subsequently authorized packaging this work as **5.4.1 Beta 1** for their device testing. See `HANDOFF_5.4.1_BETA1.md` for current release identity and publication confirmation. The implementation/test evidence below describes the preceding local development run; live-device limitations still apply.

## Previous weaknesses found in source

1. Capture allocation sometimes used the app's resource/current-window metrics rather than the full display. Gesture dispatch nevertheless treated image coordinates as physical screen coordinates.
2. `GameViewport.fit` invented a centred 9:16 game on every wider image, without examining its pixels. A real tablet/adaptive game window and a pillarboxed game were consequently indistinguishable.
3. The DWS grid cache was not tied to a display/capture/window/DPI generation. Delayed movement bursts could use coordinates from a previous geometry.
4. Projection resize/configuration changes did not resize/rebind the existing capture surface. Android 14 app-only sharing could also introduce an ambiguous origin.
5. Padding and plane strides were passed through to analyzers, including potentially incomplete last-row padding. Very small Plus symbols were tested with a forced 3x3 patch, which erased thin cross arms.

## Shared coordinate contract

`PhysicalDisplay` is now the common display source for allocation and input validation. The active game application window is read from Android accessibility window bounds. Inside that window, observed near-black outer strips are excluded; aspect ratio and device names do not establish a crop. Own quick-control overlays are excluded from this border observation, and overlay-covered gesture paths are rejected.

Full-display projection is requested explicitly on Android 14+. `ProjectionTransform` models uniform scale and centred surface padding, as documented for Android 12L+. Older Android versions reject an inconsistent surface aspect and request resize rather than assume stretching. [Android MediaProjection documentation](https://developer.android.com/media/grow/media-projection).

All existing image analyzers receive an `AnalysisImage` in **game-local coordinates**. RGBA plane data is cropped/compacted with its real row/pixel stride; no device-specific resolutions are stored. The final gesture uses the same frame's immutable geometry:

`displayPoint = projection.toDisplay(gameCropOrigin + analysisPoint)`

The grid overlay uses that same origin/scale, including its own Android window offset. Invalid/nonfinite/out-of-game points are rejected, not clamped onto another control.

## Lifecycle and safety

- Three matching observations are required. Geometry is session-local; no previous calibration is restored from preferences.
- A changed display/window/crop/capture size/DPI/rotation revokes the old generation immediately. A frame gap longer than two seconds also requires fresh confirmation, even if the next geometry is identical.
- Android display/configuration and captured-content resize events resize the existing virtual display/surface. They do not reuse a projection token to create another virtual display.
- Each gesture validates the current physical display, game window, DPI, foreground package and calibration generation. Its completion means Android delivered input, **not** that the desired game action succeeded.
- Queued DWS bursts retain their original generation and are discarded when it changes. DWS additionally rechecks observed grid geometry periodically on a frame without its own debug-grid drawing; changed geometry requires fresh grid calibration. Three inconclusive periodic checks revoke the cache. Feature state machines continue to require their existing visual postconditions.
- Longer unconfirmed geometry produces an explicit localized calibration/input-paused status. Changed/closed capture frames are discarded without crashing the analysis thread.
- Opt-in diagnostics include generation, display/game-window bounds, capture/crop/analysis sizes, origin and scale, actual mapped gesture coordinates, and input rejection reasons. `geometry-current.json` remains available even after the general context snapshot budget is consumed. Existing paired screenshots remain bounded to 50/session.

## Tests actually executed

**672 JVM tests, zero failures/errors/skips**; signed release-variant build and vital lint successful. The previous baseline was 326 tests; 346 additional tests cover this work. Earlier test failures exposed the landscape area gate and thin Plus sampling problem and were fixed before the final successful run.

| Coverage | Executed evidence |
| --- | --- |
| Display sizes | 1080x2340, 1080x2400, 720x1612, 1440x3200, 1080x1920, 1536x2048, 2560x1600, 480x800 |
| Geometry matrix | 72 combinations x 4 assertions/tests: no/gesture/144-pixel navigation, status offset, asymmetric black margins, native/half-size/720x1280 capture surfaces |
| Mapping/action simulation | Interior/corner targets round-trip to the expected physical pixels; invalid/out-of-game targets rejected; independent physical hit-test changes synthetic button state only after the transformed click lands |
| Lifecycle/format | Session reset, 2340-to-2400 change, DPI/window/rotation changes, frame gap, old queued bursts, black loading frames, overlay exclusion/path intersection, RGBA row/pixel strides and absent last-row padding |
| Recorded screenshot replay | 16 display/capture combinations x 3 tests: collapsed/expanded Partner and 15 roster slots; Network ad/Matching; Meat Field seed dialog, three slots and selection button. Images are uniformly fitted inside simulated windows, then processed through observed crop and feature detectors |

The two old tests that asserted an unconditional 9:16 crop now assert that dimensions alone cannot prove a crop; an additional observed-black-border assertion retains legitimate pillarbox coverage. Reward-offset tests still check the original physical target position, now by converting the returned viewport-local target through the measured viewport.

## Limits and required follow-up

- **No physical phone or emulator gameplay test of this new code was performed.** Only read-only device inventory was used; the connected device was BlueStacks `emulator-5554`. No display settings, installed packages or ongoing game run were changed.
- Software DPI/configuration changes test invalidation and mapping, not actual Android settings callbacks. Recorded-image resizes do not simulate Android/game font reflow, new UI content or every renderer/OEM implementation.
- Border detection deliberately recognizes near-black outer strips, not arbitrary textured/gray backgrounds. A new/different game layout still needs positive feature recognition. The transform does not make an unknown screen recognizable by itself. Truly black gameplay edges, severe overlay obstruction and unsupported capture semantics may cause a safe pause; this is preferable to invented coordinates.
- Periodic DWS revalidation needs live performance/animation testing. Existing Classic/All-Sprite movement rules and feature decisions were intentionally retained; this work is not proof that every Dungeon/Bond outcome is fixed.
- Next validation: install the explicitly labelled local development APK on a consenting test device, keep USB debugging disabled on phones, run the in-app diagnostic mode, check Plus/roster/bubble/seed/ad/Matching targets and visual postconditions, then restart after real resolution/zoom/navigation changes. Gather paired before/after frames with `geometry-current.json` for failures. Do not add model-specific profiles.
- At initial implementation, app ID/signing identity/settings and version 84/5.3.0 were unchanged. That local build is **not** the published 5.3.0 asset. The subsequently authorized Beta uses version 85/5.4.1-beta.1 with the same app ID/certificate/settings; see its handoff for verified publication details. Do not replace old tags/assets or publish another version without explicit authorization.
