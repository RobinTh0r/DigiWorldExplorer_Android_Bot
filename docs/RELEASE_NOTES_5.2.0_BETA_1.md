# DigiWorldExplorer 5.2.0 Beta 1 — Digital World Profiles

This preview makes Digital World behavior predictable across old and new device profiles while retaining the physical-phone fixes developed after 5.1.0.

## Exclusive Digital World profiles

- **V3 Classic** restores the original rules with Botamon-only detection and without Force Forward, Dash Spam or Collect Only Energy.
- **V4 Dash** is the default. It uses Botamon-only detection, all three V4 rules and the original three-tap/800 ms movement rhythm.
- **V5 All Sprites + Dash (Test)** combines all three Dash rules, general sprite recognition and the safer two-tap/1.1 s movement rhythm for modern phones.
- Only one profile can be active. Switching profiles resets grid tracking immediately.

## Physical-phone reliability

- Includes the latest Partner selection, Bond bubble, Meat Field, Home reward and Touch-to-Start retries and recognition fixes.
- Samsung S22 Ultra diagnostics showed that an exhausted Dash `0` can become visually unreadable. An unreadable or zero counter now authorizes no Dash in every profile, preventing accidental entry into World Search.
- V5 uses the safer two-step movement cadence while V4 deliberately retains its established movement rhythm.

## Safety and validation

- Unknown counters never authorize Dash, tickets, advertisements or premium actions.
- Classic Auto-Summon remains on the proven 4.0.0 recognition and click path.
- Automated unit and screenshot-regression tests pass; physical device validation should continue through offline diagnostic ZIPs when USB debugging causes the game to close.
