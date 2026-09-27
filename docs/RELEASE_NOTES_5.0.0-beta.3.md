# DigiWorldExplorer 5.0.0 Beta 3 – Co-Pilot Reliability Preview

> **Preview:** This build is still under active development and has currently been tested only
> with BlueStacks. Supervise automation runs and report screens that stop or are misclassified.

## Digi Co-Pilot

- Fixed Digital World Search becoming stuck immediately after it started. The DWS analyzer could
  access capture-plane metadata after Android had closed the frame, terminating frame analysis.
- Detected Bond bubbles are tapped immediately and the next partner follows after a short settle.
- When no bubble is detected, the rotation waits six seconds before using a bounded fallback sweep.
- Fixed collection evidence from the previous partner causing the next partner to be skipped.
- Keeps the 15-partner rotation, starting-partner restore, Meat Field and Home reward phases.
- The persistent 20-minute Bond timer continues across app restarts; DWS uses only its remaining
  wait time and returns when the timer is ready or the five-minute limit is reached.

## Dungeon Co-Pilot

- Runs a user-started route through the selected dungeons.
- Supports up to three normal attempts and up to two Ad Skip attempts per regular dungeon.
- Handles supported battles, result screens, rewards and return navigation.
- Apocalymon remains configurable but disabled by default.

## Known limitations

- Tested only with BlueStacks so far; physical phones, tablets and other aspect ratios still need
  validation.
- Dungeon selection synchronization and return-to-Home behavior require further testing.
- This preview does not promise unattended or error-free automation.
