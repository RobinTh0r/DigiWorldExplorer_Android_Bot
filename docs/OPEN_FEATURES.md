# Current automation status

Updated 2026-09-25 after the live Dungeon pass.

### 2026-09-25 scroll recognition correction

- Reproduced the user's bottom-list screenshot: the card reader recognized all five cards,
  but KnownPageDetector rejected the page, leaving the rotation inactive with an Explore label.
- KnownPageDetector now shares the proven top/bottom card reading. Screenshot regression added.
- Gesture completion now includes a settling interval; Start Dungeon Rotation resets stale executor state.
- The battle guard now permits result-only processing, so reward closure is no longer starved.
- Network team-notice confirmation has priority over the background Challenge button; after confirmation,
  the executor selects Matching. Entry and confirmation screenshots cover the distinction.
- Full unattended daily rotation, ad counters and Network party-return handling still require live validation.

### 2026-09-25 full rotation verification

- Verified resume from an already-open Bakemon modal.
- Verified separate Ad-Skip ticket grant, reward close and subsequent battle start.
- Verified bounded retry when the game swallows a Start tap during a transition.
- Verified Digifactory two Ad-Skip attempts and both battles.
- Verified Network Defense two Ad-Skip attempts, Matching/battles and team disband confirmation.
- Verified Metal Sea normal/Ad-Skip battles, VS zero-resource completion and return to Home.
- Added visual fallback: returning to the card panel releases a missed reward callback instead of
  parking indefinitely in `waiting`.
- Release APK SHA-256: `1CC378B110D251A785220AD3B4FEDBC04D24C905B60F0C7820A192FB950CEFEC`.

## Live-functional

- Screen capture, exclusive frame ownership, movable status overlay and eye states.
- Digital World Search core navigation and combat support currently retained from 4.0.1-beta.3.
- Meat Field harvest, seed priority, optional watering and global Ad Skip Pass gating.
- Fifteen-partner Bond tour, starting-partner restore, one post-tour Meat Field visit and global
  20-minute cooldown.
- Existing VS/Tower in-battle loop and reward closing.
- Dungeon Home -> list navigation via the manual overlay action.
- One live successful run through Apocalymon, DemiDevimon, Bakemon, Digifactory, Network Defense
  (including Matching) and Metal Sea. VS zero-resource handling was safely verified.
- Dungeon settings model/UI: individual cards, one/two normal attempts and optional two ad attempts.

## Partially functional / next priority

1. Wire `DungeonRotationSettings` and `DungeonBudgetLedger` into the live list executor.
2. Add stable top/bottom scroll proof and per-card identity/counter reading.
3. Automate card return and continuation after reward/loss; handle Network Matching and team exit.
4. Detect the transition from normal tickets to two ad attempts and require both the global pass and
   a positive visual ad counter.
5. Persist daily Dungeon progress/reset and expose a rotation summary.
6. Hand the VS card to the existing VS module only after a positive Destroy resource is proven.

## Other open product work

- Title/Touch-to-Start and idle-reward controllers exist as pure/tested logic but are not connected
  to live capture and navigation yet.
- Gekkomon Run remains deliberately last: event navigation, obstacle tracking/timing, result/quit,
  Fever target and daily cap are incomplete.
- Full Autopilot still needs one shared task scheduler connecting Bond, Farm, Dungeon, login/idle,
  Summon and World Search with verified Home returns.
- Configurable task order, run-now actions for every task, persisted daily summaries and safe resume
  data remain open.
- Flexible Digital World player sprites beyond the currently tested profile need calibration UI and
  real screenshot sequences.
- Release hardening still needs multilingual/multi-resolution fixtures, long state-machine runs and
  physical-device coverage.

## Safety boundary

No unreadable counter grants a spend. Unknown results consume the conservative reservation and park
the affected task. Apocalymon is capped at one run per rotation. VS Destroy requires a positive
resource count. Ad-labelled actions require the persisted global Ad Skip Pass setting.

## Planned daily checklist UI

- Add a compact “Today” overview showing every Dungeon/task as open, running, completed, skipped or
  unsafe, plus attempts/ads used where known.
- The underlying Dungeon day already persists completions and rolls over at exactly 08:00 in
  `Europe/Berlin`; Java time-zone rules keep this at German local 08:00 across summer/winter time.
- Apocalymon completed on the 2026-09-25 live pass is recorded for that game day and must not run
  again before the 2026-09-26 08:00 reset.
