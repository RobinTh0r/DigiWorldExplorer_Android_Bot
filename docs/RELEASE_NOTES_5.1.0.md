# DigiWorldExplorer 5.1.0 — Phone Automation Reliability

Version 5.1.0 promotes the physical-device fixes developed through the 5.0.2 and 5.0.3 previews. It focuses on OnePlus-class tall displays while retaining the established emulator layouts.

## Dungeon Co-Pilot

- Uses visible list positions, buttons and dialogs instead of device-specific tap coordinates.
- Handles Apocalymon, DemiDevimon, Bakemon, Digi-Factory and Network Defense transitions, including Matching and the tall Digi-Factory Ad-Skip reward sheet.
- Distinguishes summon/result lookalikes from Dungeon and stops when required ticket or Ad-Skip evidence is unreadable.
- Two supervised Dungeon rotations completed on the supplied OnePlus 8 Pro diagnostic run.

## Digi Co-Pilot

- Recognizes the tighter OnePlus 5×3 Partner roster, verifies all 15 cells and uses the green active-partner marker to confirm switches.
- Moves the lower-right Partner tap away from the roster control and gives switches time to settle.
- Widens Bond-bubble recognition and aims at the associated Digimon with a short collection burst.
- Keeps an empty or unresolved Meat Field active until it can plant or capture diagnostic evidence. OCR fluctuations no longer prevent a visually confirmed empty plot from opening the seed menu.

## Classic Auto-Summon

- Restores `RewardPurchaseDetector` and `RewardPurchaseFrameAnalyzer` behavior from version 4.0.0 after experimental replacements caused false menu recognition, card taps and slow result waits.
- Restores the original 200 ms cadence, five-second continuation window, Crest confirmation window and optional touch correction.
- Runs classic Summon before generic entry/reward guards so those analyzers cannot click a result card or consume animation frames.
- Supplied green-ticket and purple/Crest captures are regression-tested. The final rollback was confirmed by the user on the OnePlus test device.

## Offline diagnostics

- Adds an opt-in mode under Experimental for testing with USB debugging disabled.
- Stores bounded action/state logs and up to 50 compressed 540-pixel-wide screenshots per session.
- Lists saved sessions, supports individual deletion, and shares one session or all retained sessions as ZIP files.
- Keeps up to six sessions and never records continuous video.

## Safety and validation

- Ad Skip Pass remains opt-in. Unknown or unreadable counters do not authorize spending.
- Unit and screenshot-regression tests pass and the release APK uses the established application certificate.
- Samsung S21 Ultra and every possible dialog were not independently exercised in the final release pass; diagnostics remain available for device-specific reports.
