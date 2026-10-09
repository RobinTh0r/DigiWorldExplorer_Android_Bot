# DigiWorldExplorer 5.3.0 Beta 3

This is a test release for the game's 1.5.0 interface. It uses the existing app ID and signing certificate, so it installs over Beta 2 without deleting settings or app data.

## Changes

- Dungeon diagnostics now include ad switches, daily usage, observed card counters and explicit reasons for skipping ad attempts. The reported missing Network Defense/DigiFactory ad attempts remain under investigation.
- Unknown Dash stock is treated as unavailable on every profile. The recorded new green stock `271` is currently unreadable by the number detector, so Dash stays disabled for that state pending counter calibration.

- Adapt DWS grid calibration, the green Dash action and its resource counter to a recorded 1.5.0 BlueStacks screen. The upper-right broom/brush action is excluded from Dash targeting. Keep the previous phone-layout fit as the first detection path. If the new Dash stock cannot be read positively, the bot does not guess that charges are available.
- Restore V4's Botamon classifier, player selection and movement decisions against the `v4.0.0` implementation. The screen geometry is shared with the updated detector because the game's UI has changed; this is not a byte-for-byte copy of the old app.
- Reject text-heavy dialog frames before using a cached DWS grid for movement. Add opt-in, sparse action screenshots tied to taps, within the existing diagnostic image cap.
- Add image regression tests for the expanded and collapsed German Partner panel in game 1.5.0. Localized quick controls and clarified the V3/V4 Botamon versus experimental V5 sprite guidance.

## What was tested

- 314 automated tests passed with no failures or skipped tests. Release build and vital lint passed; the APK uses the existing signing certificate. BlueStacks upgrade installation and app startup were checked. This is not a full end-to-end validation of every game feature.

- On German BlueStacks, one Bond Rotation partner switch reached selection, raising, confirmation and Home. There was no visible food/bond bubble in that run, so bubble collection was **not** tested.
- DWS 1.5.0 detection and the new Partner-panel targets were checked with captured screenshots and automated regression tests.
- Existing activation-code validation and storage are unchanged. V3/V4 remain free; V5 still requires Beta access. The activation check is a code review, not a claim that every automation feature is reliable.

## Still under investigation

This Beta is **not** verified on every phone. Physical-phone DWS and Bond behavior, bubble collection, Dungeon early returns and free/ad attempts, Network Defense, Metal Sea and Auto Summon reports remain open. More unverified center taps were deliberately not added to Bond Rotation because they can open the Digimon underneath a disappearing bubble. Please use Experimental diagnostics through a complete failure on affected phones. If USB debugging makes the game exit with security error `00000038`, keep it off while playing and share the in-app diagnostic ZIP.
