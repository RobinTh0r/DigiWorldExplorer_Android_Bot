# DigiWorldExplorer 5.0.3 Beta 2 — OnePlus Diagnostics & Co-Pilot Fixes

This supervised-testing preview follows real-device runs on a OnePlus 8 Pro. Version 5.0.1 remains the stable download.

## Dungeon Co-Pilot

- Recognizes the tall translucent Digi-Factory Ad-Skip reward sheet and closes its language-independent tap-to-close area.
- Resumes the active dungeon transaction only after the reward transition and returned challenge panel are visually confirmed.
- Captures additional diagnostic evidence when a ticket counter is unreadable instead of treating an unreadable counter as permission to spend.

## Digi Co-Pilot and Bond Rotation

- Corrects the tighter 5×3 Partner roster row geometry seen on OnePlus 1080×2376 captures.
- Verifies all 15 visible cells and the active partner before selecting the next partner.
- Keeps the compact overlay behavior while avoiding false conclusions from the expanded menu: the observed failure was roster geometry, not a missed `+` tap.

## Offline diagnostics

- Adds an opt-in mode under Global Settings → Experimental for testing with USB debugging disabled.
- Stores bounded state/action logs and targeted 540-pixel-wide JPEG screenshots without saving continuous video.
- Keeps up to six sessions with 24 screenshots each. Sessions can be shared as ZIP files through Android's share sheet or deleted individually.
- Reduces duplicate status entries and reserves screenshots for meaningful actions, transitions and failures.

## Preview notice

All unit and screenshot-regression tests pass, including the supplied OnePlus Dungeon reward and expanded Partner roster captures. Continue to supervise Dungeon and Digi Co-Pilot runs. Ad Skip Pass remains opt-in, and unreadable counters never authorize spending.
