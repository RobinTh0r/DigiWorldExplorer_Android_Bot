# Changelog

## 5.0.2 Beta 2 — 27 September 2026

- Fixed Touch to Start recognition on OnePlus 8T Pro and current Galaxy tall-screen layouts.
- Reworked tall-phone dungeon panel anchors and counter regions for Apocalymon, DemiDevimon and Network Defense.
- Fixed positive counters such as `4/2` being interpreted as zero because of the shape of digit 4.
- Added Bond recovery when a fast device switches partner between capture frames and the confirmation dialog is never observed.
- Verified title, dungeon panels and active Partner state directly against five supplied 582×1280 device captures.

## 5.0.2 Beta 1 — 27 September 2026

- Added adaptive tall-phone coordinate mapping for current 19.5:9 and 20:9 Android displays.
- Fixed Digi Co-Pilot opening the Partner roster but missing the `+` control or stopping on the expanded roster on OnePlus devices.
- Added tall-layout Partner header, roster, active-partner and navigation detection shared by Galaxy S/Ultra, Pixel and OnePlus-class displays.
- Fixed Dungeon Co-Pilot list recognition on tall phones, including the shifted Dungeon header and wider card geometry.
- Kept the established 9:16 BlueStacks profile separate to avoid changing its proven targets.
- Preview notice: Bond and Dungeon were verified against supplied OnePlus captures; Meat Field and reward flows use the corrected common viewport but still need supervised device testing.

## 5.0.1 — 27 September 2026

- Digital World Search now recognizes varied partner sprites instead of depending on Botamon's yellow eyes. General shaded-body recognition supports forms such as WarGreymon and MetalGarurumon while retaining the existing eye detector and movement safeguards.
- Stale DWS calibration and grid overlays are released quickly after leaving the board.
- Colourful partner sprites are no longer added to the collectible exclusion list.
- A status-panel switch is available beside the overlay control in the main app without Beta access.
- Added regression tests for non-Botamon partners and text-heavy screens sampled through stale grid bounds.

## 5.0.0 — 27 September 2026

- Added Digi Co-Pilot: Bond Rotation → Meat Field → Home rewards → Digital World Search.
- Added Dungeon Co-Pilot with selected dungeon routing, normal attempts, optional Ad-Skip attempts, reward handling and return navigation.
- Added full 15-partner Bond Rotation with bubble collection, starting-partner restore and a persistent 20-minute timer.
- Added Meat Field harvesting, prioritized planting and watering automation.
- Added once-daily Apokalymon handling with result confirmation and an 08:00 Europe/Berlin reset.
- Added compact in-game Start/Stop controls, bot-eye states, persistent status information and VS/Tower access.

## 4.0.0 — 26 August 2026

- Added the movable quick-control overlay and combined VS/Tower Loop.
- Added dark mode, guided troubleshooting and advanced DigiWorld settings.
- Improved mode hand-offs, screen detection and device compatibility.
