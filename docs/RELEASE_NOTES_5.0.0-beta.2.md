# DigiWorldExplorer 5.0.0 Beta 2 – Automation Preview

> **Preview notice:** This is an early look at the next generation of automation—not a finished or
> stable release. It has currently been tested only with BlueStacks. Supervise each run and expect
> screen recognition, timing and individual flows to need further refinement.

## Digi Co-Pilot (Preview)

- Starts and stops from the round in-game bot overlay.
- Coordinates only the phases selected by the user.
- Runs the Bond Rotation through up to 15 partners and attempts to collect each detected Bond bubble.
- Restores the starting partner and stores the 20-minute cooldown locally across app restarts.
- Can then check Meat Field, collect Home rewards and use the remaining wait time for a bounded
  Digital World Search.
- Returns toward Home between phases and waits for the next cycle when all enabled work is complete.

### Included Digi Co-Pilot phases

- **Bond Rotation:** partner switching, bubble detection and collection, starting-partner restore.
- **Meat Field:** harvesting, planting by seed priority and watering by Palmon priority.
- **Home rewards:** opens the reward chest and handles the receive/result steps.
- **Digital World Search:** optional activity during the remaining Bond cooldown; ends when the
  cooldown is ready or the bounded run time is reached.

## Dungeon Co-Pilot (Preview)

- Runs one user-started route through the dungeons selected in Settings.
- Uses up to three normal attempts per regular dungeon.
- Can use up to two additional ad attempts when the global Ad Skip Pass option is enabled and the
  game reports them as available.
- Handles supported challenge dialogs, battles, losses, result screens, rewards and the return toward
  Home.
- Temporarily owns the automation flow so competing screen actions do not interrupt the dungeon run.
- Keeps the daily special handling for Apocalymon and VS Destroy.
- **Apocalymon remains available as an option but is disabled by default in this preview.**

## Interface

- Compact Beta Automation section with separate configuration and correct help for every module.
- Beta actions grouped in the round bot overlay.
- Persistent Bond timer and explicit waiting/status messages.
- Classic VS/Tower Loop remains separate from the user-started Dungeon Co-Pilot.

## Known limitations

- Tested only with BlueStacks so far; physical phones, tablets, other resolutions and aspect ratios
  still require validation.
- The Dungeon Co-Pilot can currently ignore a recently changed card selection and start at
  Digifactory even when DemiDevimon or Bakemon were enabled. Reopen the settings and supervise the
  route until this synchronization issue is fixed.
- Starting or finishing a Dungeon route from Home can incorrectly trigger the title-screen recovery
  (“closing open dungeon”). This may press Back repeatedly even though Home is already visible.
- After VS was completed once and another rotation is started, the return-to-Home state may repeat
  title-screen messages instead of ending cleanly.
- Screen recognition and timings can still stop or misclassify a flow.
- Apocalymon needs additional testing and is therefore off by default.
- This preview does not promise unattended or error-free automation.
