# Changelog

## 5.2.0 Beta 2 — 1 October 2026

- Fixed Bond Rotation on physical phones by tapping only positively detected Bond bubbles and advancing only after the tapped bubble disappears.
- Removed blind centre and figure-offset Bond taps that could open a Partner detail popup or skip an uncollected partner.
- Fixed the translucent Meat Field seed dialog remaining hidden behind the still-visible field detector on OnePlus-class layouts.
- Located seed counters from the detected dialog slots so planting adapts to device-specific dialog padding while preserving harvesting and watering.
- Made Stop fully release automation, MediaProjection and every app overlay; a later start restores the saved overlay choices.
- Streamlined diagnostics with a solid green active state, larger list/export controls, readable timestamps and Share All in both session lists.
- Added OnePlus diagnostic regression fixtures for Bond bubbles, the false Partner popup and the seed-selection dialog.

## 5.2.0 Beta 1 — 30 September 2026

- Replaced independently combinable Digital World switches with three mutually exclusive profiles: V3 Classic, V4 Dash and V5 All Sprites + Dash.
- Made V4 Dash the default profile, retaining Botamon detection, all three V4 rules and the original three-tap movement rhythm.
- Added the V5 test profile with all-sprite recognition, Dash and the latest physical-phone reliability fixes.
- Applied Samsung S22 diagnostics globally: a zero or unreadable Dash counter never authorizes Dash, preventing accidental World Search entry.
- Included post-5.1.0 Co-Pilot, Partner, Bond bubble, Meat Field, Home reward and title-screen reliability fixes.
- Added regression coverage for profile mapping, legacy versus all-sprite recognition and safe Dash handling.

## 5.1.0 — 29 September 2026

- Added image-driven Dungeon rotation for tall phones, including dynamic list/card targets, Network Defense Matching, Digi-Factory reward handling and safer result transitions.
- Improved Digi Co-Pilot Bond rotation with OnePlus 5×3 roster recognition, verified active-partner changes, safer lower-right selection and more tolerant Bond-bubble collection.
- Improved Meat Field recognition and planting: unknown plots no longer count as finished, changing OCR counters no longer block a verified empty field, and unresolved fields create diagnostic evidence.
- Restored the proven 4.0.0 classic Auto-Summon detector and click cadence after physical-device diagnostics exposed false ticket-header matches and card taps in the experimental replacement.
- Added opt-in offline diagnostics with event logs, up to 50 compressed screenshots per session, individual/all-session ZIP sharing and deletion from the app.
- Added OnePlus regression captures for Dungeon rewards, Partner grids, Summon ticket/Crest results and false-positive screens.
- Kept Ad Skip Pass opt-in and continued to block actions when a required counter cannot be read.

## 5.0.3 Beta 2 — 28 September 2026

- Added an opt-in offline diagnostic mode for physical-device testing without ADB, with bounded event logs and targeted compressed screenshots.
- Added an in-app diagnostic-session list with ZIP sharing through Android's share sheet and individual deletion.
- Recognized the tall translucent Digi-Factory Ad-Skip reward sheet and closed it before resuming Dungeon Co-Pilot.
- Corrected the expanded 5×3 Partner roster geometry for OnePlus 1080×2376 captures so Digi Co-Pilot can select and rotate partners.
- Reduced repetitive diagnostic status entries and prevented rapid `BOND`/`NONE` ownership changes from consuming all screenshot slots.
- Added regression fixtures from supervised OnePlus 8 Pro Dungeon and Partner runs.

## 5.0.3 Beta 1 — 28 September 2026

- Improved Digital World Search grid recognition for MuMu's dark-city board layout.
- Kept highlighted, reachable tiles traversable and prioritized nearby training points over distant energy.
- Limited the five-minute DWS excursion to Digi Co-Pilot; standalone DWS no longer inherits a stale Co-Pilot timeout.
- Added a Summon reward-screen guard to stop repeated taps or purchases while cards are revealing; Auto Summon now defaults off for new installs.
- Made VS/Tower Loop and overlay visibility controls available without Beta access and removed the obsolete fixed corner status panel.
- Set unset Premium defaults for Bond, Meat Field, rewards and Co-Pilot DWS, while preserving explicit choices; enabled dungeon selections and three normal attempts by default. Ad Skip Pass remains opt-in.
- Added screenshot-based regression tests. Physical Oppo, MuMu and other device flows still require supervised testing.

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
