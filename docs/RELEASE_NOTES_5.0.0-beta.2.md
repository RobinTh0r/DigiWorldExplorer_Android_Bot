# DigiWorldExplorer 5.0.0 Beta 2 – Early Next-Gen Test

> This release is intentionally an early test build—closer to an alpha preview than a polished
> beta. The new automation flows and UI still need device testing and optimization. Please supervise
> the first runs and report where a screen is not recognized or a flow stops.

## Dungeon Co-Pilot (Beta)

- Runs the selected dungeon route from the in-game overlay and returns toward Home.
- Uses up to three normal attempts and, with Ad Skip enabled, up to two additional free attempts.
- DemiDevimon and Bakemon therefore receive at most five attempts before the next dungeon is tried.
- Temporarily suspends the separate Network Defense feature while it owns the dungeon run, avoiding
  a competing Diaboromon/end-boss loop. The previous setting is restored after Stop or completion.
- Apocalymon is disabled and greyed out in Beta 2 because its opened panel does not yet produce a
  sufficiently reliable Start transition.
- A configurable endless retry loop similar to the Classic VS/Tower loop is planned, not included.

## Digi Co-Pilot, Bond and Meat Field (Beta)

- Runs the 15-partner Bond rotation, restores the starting partner and keeps the 20-minute timer.
- Scans immediately and at high frequency for the first 15 seconds after each partner switch, but
  taps only after positive Bubble detection. It waits another second after a confirmed tap before
  opening the next partner and keeps a slower bounded fallback window afterward.
- Can continue with Meat Field, Home rewards and an optional bounded Digital World Search phase.
- Meat Field harvests, replants and prioritizes available watering actions.
- Digi Co-Pilot respects the modules selected in the overlay instead of silently enabling Bond or
  Meat Field. Disabled modules are skipped deliberately.
- Reward result/receive screens are retried at a bounded interval when the game swallows the first tap.

## UI and controls

- New automation modules are grouped in a compact Beta block with configuration and info actions.
- Beta-only toggles and Co-Pilot buttons are visibly marked in the in-game overlay.
- The Classic VS/Tower Loop switch is available on the main screen again.
- Tap the round bot to open its controls; hold it for two seconds to toggle the status bubble.

## Known limitations

- Apocalymon is intentionally unavailable in this build.
- Dungeon and DWS screen recognition may still require tuning on devices and aspect ratios not yet
  covered by the BlueStacks test set.
- This prerelease is intended to show and test the Next-Gen direction, not to promise unattended,
  error-free automation yet.
