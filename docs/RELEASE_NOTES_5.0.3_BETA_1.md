# DigiWorldExplorer 5.0.3 Beta 1 — DWS & Device Reliability

This supervised-testing preview addresses reports from Oppo Reno12 Pro and MuMu Player while retaining the modern-phone layout work from 5.0.2 Beta 2. Version 5.0.1 remains the stable download.

## Digital World Search

- Recognizes the dark-city MuMu grid more reliably using its bottom board anchors.
- Treats highlighted blue walkable tiles as traversable and favors reachable nearby training points before distant energy.
- Keeps the five-minute DWS excursion exclusive to Digi Co-Pilot. Standalone DWS no longer stops because of a stale Co-Pilot request.

## Summon safety

- Detects card-reward and reveal screens to prevent repeated right-side taps and unintended additional purchase attempts.
- Auto Summon defaults to off for new installations; an existing explicit setting is preserved.

## Controls and defaults

- VS/Tower Loop and the overlay visibility control no longer require Beta access.
- Removes the obsolete fixed top-left status panel; the compact panel beside the bot icon remains.
- New or unset Premium settings enable Bond, Meat Field, rewards and Co-Pilot DWS, plus all dungeon selections and three normal attempts. Existing explicit choices remain unchanged. Ad Skip Pass stays off unless deliberately enabled.

## Preview notice

Unit tests and screenshot-based regressions pass, including the supplied MuMu and training-point captures. The Oppo, MuMu and other real-device flows have **not** been fully end-to-end verified in this build. Please supervise automation, especially Summon purchases and reward screens, and report device model plus a screenshot when recognition fails.
