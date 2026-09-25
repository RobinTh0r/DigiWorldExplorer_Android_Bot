# DigiWorldExplorer – Ultimate Automation Plan

Status: active implementation plan
Last updated: 2026-09-25
Primary target: `app/` (DigiWorldExplorer)
Reference implementation under analysis: `DigiautoTap/` (decompiled APK; never ship it)

## 1. Purpose and hand-off contract

### Next accepted scope: Dungeon menu-only audit

- After Bond/Farm and login/idle work, inspect Home bottom Dungeon navigation, the dungeon list,
  individual available detail/preparation menus, and verified back/list/Home transitions.
- User currently has no free dungeon tickets. This audit authorizes menu navigation and screenshots,
  NOT ticket purchases, paid attempts, ad attempts, or battle starts. Do not press an ambiguous
  Start/Challenge button just to find out whether it spends a resource.
- Use the DigiAutotap behavioral inventory as a comparison and implement independently in the
  existing detector/controller architecture. Record per-menu identities, safe targets and fixtures.
- Check start/wait state logic with pure tests where possible, but leave live battle entry, waiting,
  results and ticket reconciliation explicitly unverified until tickets are available.

### Next accepted scope: login and idle rewards (after Bond/Farm acceptance)

- Display the recognized title/login screen by name. Loading alone must never trigger a tap.
- Wait for positively recognized `Touch to Start`, tap once, then verify the next screen;
  retry only with fresh matching evidence and a bounded retry budget.
- Recognize the optional idle-reward dialog, collect normal rewards, and verify dismissal/Home.
  Video reward actions require the persisted Ad Skip Pass setting plus a verified positive
  remaining count; re-read after each claim, never assume two claims are always available.
- Dismiss recognized optional update notices/promotional popups only through their verified
  close controls. Mandatory updates, purchases, account/consent dialogs and unknown messages
  are not generic close targets and must pause rather than accept anything blindly.
- These screens may not appear in the current test session. Add fixture/state-machine coverage
  and explicitly distinguish implemented behavior from live-tested behavior; do not mark them
  verified merely because the Bond test succeeds.

### 2026-09-25 Bond repeat / Farm handoff

- Current user priority: finish and live-test the complete Bond loop before expanding features.
- Preserve the previous agent's bounded Home OPEN retries and MAX-partner right-side Start target.
- After 15 verified switches, the initially active green-check partner is restored last. The tour
  requests one immediate enabled Farm visit; existing ten-minute periodic visits remain available.
- Removed timer-only Bond restarts. At Home, REST must observe no bubble and then a new bubble
  before opening the next tour. A still-visible final bubble cannot immediately restart the tour.
- Unit coverage includes all 15 unique targets, original restoration, no timed restart, a new
  bubble trigger, and a one-shot post-tour Farm request that waits for a safe Home boundary.
- Full unit suite: 175 tests passed; signed `assembleRelease` passed and APK installed with `-r`,
  preserving package/version/signature/settings. The additional stale-bubble regression also passed.
- Live attempt 2026-09-25 02:59:34–03:02:30: start partner index 1; eight verified switches to
  indices 2..9, with Home returns and bubble taps. Screenshot after collection showed no bubble.
  This is NOT a completed 15/15 run and does not prove every token credit.
- At 03:02:51 Home OPEN stopped producing transitions; bounded retries fired twice. Subsequently
  ADB screenshots became entirely black, including the overlay. Android display remained ON;
  game PID 4214 remained alive, and launcher was briefly foreground. Bringing the existing game
  activity forward restored its Android focus but NOT screenshot content. Asked user to make
  BlueStacks visible/unlock display; cause remains unconfirmed. Do not blindly tap the black image.
- Pending: restore original index 1 (last verified active index 9), then an uninterrupted 15/15
  live run, immediate Farm handoff/Home return, and bubble-driven restart. Current safe timeout
  parks the failed tour; restarting capture starts a NEW tour and forgets the old original.

### 2026-09-25 verified global Bond cycle

- User changed the repeat contract: no bubble-driven or ten-minute restart. A successful 15/15
  tour restores its original partner, requests exactly one Meat Field visit, returns Home, then
  starts a global 20-minute monotonic cooldown. Bond cannot start during that cooldown.
- The compact overlay shows the live remaining time on its second line (`Bond mm:ss`). The timer
  begins only after the field has completed and Home is positively recognized, not after 15/15.
- Route classification no longer treats unsolicited field-like geometry as a Farm destination.
  Explore and known Partner/Dungeon pages are excluded before Farm detection; a field can enter
  the Expedition route only while its post-Bond visit is explicitly pending.
- Live final run on 2026-09-25: original partner index 0; all 15 switches were confirmed and index 0
  restored. The app then opened Explore and Meat Field once, harvested the one newly ripe plot,
  replanted it, closed the field, returned Home, and displayed `Bond 19:32` while staying Home.
- The prior live attempt exposed a completed planted plot becoming visually UNKNOWN during an
  animation. FarmController now remembers per-visit, visually proven plantings; later animation of
  that same plot cannot block completion, while an unrelated UNKNOWN plot still cannot be tapped or
  silently accepted. Full suite and signed release gate passed at 184 tests before final install.

### 2026-09-24 continuation after Codex usage limit

- Active source is `.release-v3.2-beta1`, branch `codex/v3.3.0-beta.1`, version
  `4.0.1-beta.3` / code 66, INCLUDING the uncommitted changes. Fusion is obsolete.
- Codex's last visible message reported 13/15 partner changes and a pending bubble-detector
  update; the session ended at the usage limit. Do not mistake the Git tag for all today's work.
- Removed Copilot's accidental `de.robinthor.digiworldexplorer.beta` 4.0.0 second installation.
  Built the active tree and updated ONLY `de.robinthor.digiworldexplorer` with the matching signed
  release via `adb install -r`, preserving main-app data. No branch merge or source replacement.
- Added two bounded OPEN retries while Home is positively recognized, retaining the original
  25-second deadline. Regression tests cover retry timing, unknown prompts and subsequent selection.
  Focused Bond tests and full `testDebugUnitTest assembleRelease` passed.
- Isolated live Bond run: original partner Sakuyamon (cell 14), six verified changes with Home
  returns and bubble-tap logs. Capture stopped at 22:15:52 with a system/user stop callback;
  an equipment comparison was visible afterwards. The precise stop cause is unconfirmed.
  This is NOT a completed 15/15 run or proof of every token credit.
- Closed the comparison with Back, without selling/equipping; manually restored Sakuyamon and
  verified the green active check. Farm was restored to enabled; Expedition/Bond, watering and
  Ad Skip settings preserved. Automation is stopped. Only the main app remains installed.
- Next: complete an uninterrupted 15-partner run and verify original restoration plus token
  outcomes. Investigate capture interruption if reproduced, then test combined Farm/Bond handoff.
- Build here using the existing `.gradle-codex-build/tools/gradle-9.4.1/lib/*` classpath with
  Android Studio JBR `java.exe`, `org.gradle.launcher.GradleMain`, and the SDK 36.0.0 AAPT2 override.
  Real ADB screenshots work; avoid UI hierarchy dumps during live automation because UiAutomation
  can disconnect/reconnect the accessibility service. Use screenshots and bounded log observation.

### 2026-09-24 farm-first verification update

- User priority: finish Meat Field before title/Touch to Start, idle rewards, Bond rotation, or Dungeon navigation.
- Fixed watering counter crop to exclude the seed row. Screenshot regression checks now read 16 and 0 correctly.
- Fixed compact bold 3 being classified as 2; real seed dialog regression reads [13, 1, 0].
- Recognized water bubbles now establish GROWING when an animated sprite obscures the timer or resembles a lock.
- All 163 unit tests passed; signed release built and installed on emulator-5554. Capture restarted via the consent UI.
- Current live field: 7 common seeds, 0 middle/right seeds, 0 watering cans, five growing plots and one locked plot. No new full harvest/plant/water cycle was possible immediately after this update.
- Remaining verification: animated Palmon colour priority across poses, exhausted-resource/error recovery, and a complete live cycle on the new APK. Do not mark farm complete yet.
- Afterwards: title recognition and Touch to Start; idle ad rewards only with enabled Ad Skip Pass and verified remaining count, then normal claim; Bond and rotation. Dungeon deferred.

This file is the durable source of truth for the rebuild. It is intentionally detailed enough that
another developer or AI can continue without relying on chat history.

When continuing the work:

1. Read this file completely.
2. Read `git status` and preserve unrelated user changes.
3. Update the progress log and checklist in this file with every meaningful change.
4. Do not copy the decompiled Java sources into the product. Reimplement observed behaviour in
   idiomatic Kotlin, using DigiWorldExplorer's existing detectors and safety rules.
5. Never add a blind tap to a destructive or paid action. Every action needs positive screen
   recognition, a bounded retry policy and visual verification of the next state.
6. Reconstruct recognition rules and workflows from reference evidence now; screenshots are not a
   prerequisite for implementation. Keep unverified actions off by default and label their status.
7. User steering: include ALL missing reference features, including quests/passive actions. Product
   mode names are now Co-Pilot and Expedition. Retain old persisted enum IDs for compatibility.

## 2. User goal

Turn DigiWorldExplorer into a full on-device automation companion that can start from the game's
home screen, choose enabled jobs, navigate into each activity, do the work, verify the result and
return home. Desired parity is broadly Digi Auto Tap plus the stronger existing DigiWorld movement
and safety behaviour from DigiWorldExplorer.

Highest priorities:

1. Meat Field: enter, harvest, plant free seeds, return home and remember when it is due.
2. Bond rotation: collect a token for every eligible partner and restore the original partner.
3. Dungeon rotation: configurable per-dungeon budgets, scrolling, tickets/ads, results and retries.
4. Gekkomon Run: open the event, play safely, track Fever progress and exit.
5. Flexible DigiWorld player detection for Botamon and tested larger sprites.
6. Full-auto task chain, useful session/daily summaries and resilient recovery.
7. A privacy-safe support/diagnostic package that another developer or AI can evaluate.

### Product modes (required, not optional aliases)

- **Co-Pilot** (formerly Semi Auto) is the safe default. The user opens an activity and the matching module may work
  only after recognizing that activity. It never performs home/menu navigation.
- **Expedition** (formerly Full Autopilot) starts only from a positively recognized clean Home screen, runs enabled and
  due tasks through verified navigation, returns Home between tasks and parks on unknown screens.

The mode is persisted independently from individual feature switches. See
`docs/SUPPORT_AND_DIAGNOSTICS.md` for support-file format and privacy rules.

## 3. What the reference app actually does

### Central pipeline

The reference uses a `Director`/`DirectorLoop` model. It classifies the current screen and delegates
to a task object (`Skill`). It has semi-automatic and fully automatic modes. Fully automatic mode
requires a clean home screen, runs configured jobs in a chain and returns home between jobs.

Important design ideas worth recreating:

- one owner per frame;
- explicit screen states rather than overlapping Boolean analyzers;
- motion/settling gates before acting;
- action → wait → recognize next state → verify;
- bounded retries and a parked/error state;
- task budgets and a repeatable task chain;
- normalized game coordinates rather than raw display pixels;
- per-task summaries and persisted deadlines/counters.

### Meat Field

The reference automatically opens the DigiWorld/Explore menu and Meat Field from home. It detects a
2×3 plot lattice, distinguishes empty/growing/ripe plots, harvests ripe plots and uses the first free
seed slot for empty plots. It verifies that a plot became empty or growing and that the free-seed
counter fell. It explicitly closes the water popup and never spends water. It stores the next due
time and a learned grow duration, then closes the field and returns home.

### Bond rotation

The reference's “Collect for all Digimon” option starts a tour after a bond token was collected. It:

1. starts only from a recognized clean home screen;
2. opens Digimon → Partner;
3. expands and reads the partner grid;
4. finds the currently raised partner;
5. selects each other partner, detects and presses Raise, confirms the dialog;
6. returns home and runs the normal token collector;
7. re-enters the partner screen for the next partner;
8. attempts to restore the originally raised partner at the end.

It retries individual transitions up to three times and refuses to tap a partner cell when the grid
is not readable.

### Dungeon rotation

The reference opens the dungeon list, recognizes cards, scrolls to top and bottom, reads per-card
ticket/ad budgets and plays selected cards in configured order. Known configured names include:

- Apocalymon Wall
- Fight! DemiDevimon
- Fight! Bakemon
- Fight! Digifactory
- Network Defense Ops
- Metal Sea
- Daily changing dungeon (currently hidden in settings)

It supports a per-dungeon attempt budget, optional ad tickets, result handling, losses, Clear Previous
Difficulty after a configured number of attempts, and return-to-list/home recovery.

### Gekkomon Run

The reference can open Events, select the Gekkomon card and start runs. During a run it detects
red/magenta obstacles and chooses Jump or Slide. It tracks motion/speed, Fever state and score, stops
actively playing at 15,000 points to avoid setting a bot leaderboard record, exits the run and starts
another until the configured daily Fever target is reached. It deliberately does not claim rewards.

### Persistence and summaries

The reference stores task configuration and selected progress, not a full event database. Examples:

- Meat Field due time, learned grow seconds and last outcome;
- per-dungeon budgets/spending and session summary counts;
- quest lock until the next reset;
- Gekkomon Fever count until the daily 08:00 reset;
- last-session counts for most tasks;
- configurable full-auto chain and repeat setting.

## 4. DigiWorld player detection comparison

### Existing DigiWorldExplorer

Current detection is deliberately tuned for the small black Botamon. Cell scoring plus temporal
constraints are strong once Botamon is found, but the initial player classifier is sprite-specific.

### Reference app

The reference is more flexible but not sprite-independent. It tries several fallbacks:

1. configured HSV colour ranges;
2. the cell with the strongest dark/low-saturation body area (minimum roughly 5%);
3. template matching for a known figure;
4. eye-pair or single-eye candidates, guarded by nearby dark body pixels;
5. rejection of candidates resembling the claw collectible.

Large dark Mega sprites can work, but there is no guarantee for arbitrary Digimon. Sprites may cross
cell boundaries, hide collectibles or resemble obstacles. The target design is therefore explicit
sprite profiles plus temporal tracking, not an “any sprite” claim.

## 5. Target architecture

```text
MediaProjection frame
        ↓
Capture health + game viewport mapping
        ↓
Frame Orchestrator (exactly one owner)
        ↓
Screen Classifier ──→ recovery/modal handlers
        ↓
Task Director (home / navigating / working / returning / parked)
        ↓
Task modules: World Search, Farm, Bond Tour, Dungeons, Summon, Runner, Network
        ↓
Verified Action Executor (tap/swipe/back + settle + expected next state)
        ↓
Session store / daily-reset store / UI status
```

### Required core types

- `FrameOwner`: global failure, network, runner, world grid, dungeon, bond, farm, summon, unknown.
- `ScreenKind`: positively recognized game screens and dialogs.
- `TaskKey`: stable IDs for all jobs.
- `TaskPhase`: idle, navigate, work, verify, return home, complete, parked.
- `TaskOutcome`: complete, nothing due, stopped, timed out, unsafe/unknown.
- `VerifiedAction`: target, allowed source screens, expected result, timeout, retry budget.
- `AutomationSnapshot`: current task/phase/status/counters for UI and logs.
- `DailyResetClock`: game reset at 08:00 local time, isolated and unit-tested.

## 6. Safety invariants

- Only one module may own and act on a frame.
- No action while a frame is moving unless the active task explicitly models continuous motion.
- No tap based only on elapsed time; require a recognized source screen.
- Paid currency, water, premium summons and destructive confirmations are denied by default.
- Every navigation tap must have an expected next screen and bounded retry count.
- Unknown screens park the active task; they never trigger a guessed “close” tap.
- Stop cancels queued actions and clears pending task state immediately.
- Feature switches persist user intent; analyzer timeouts reset a run, not the switch.
- Coordinates must be derived from a detected game rectangle or normalized display mapping.
- New detectors need pure JVM tests plus real screenshot fixtures before being marked stable.

## 7. Implementation phases

### Phase 0 – Repository/build baseline

- [x] Fetch and check out the original repository `main` branch.
- [x] Keep `DigiautoTap/` untracked as analysis-only reference material.
- [x] Restore/add the Gradle wrapper (`gradlew`, wrapper JAR/properties) using the project-compatible
      Gradle version.
- [x] Run the existing unit suite and record the baseline.
- [x] Produce a debug APK before major architectural edits.

Acceptance: a clean command builds and tests the unmodified baseline plus current feature work.

### Phase 1 – Frame ownership and orchestration

- [x] Add a pure, unit-tested `FrameOrchestrator` that evaluates ordered probes and short-circuits
      after the first owner.
- [x] Move the analyzer priority logic out of `ScreenCaptureService`.
- [ ] Introduce observable automation/task snapshot state.
- [ ] Preserve existing behaviour for World Search, Stage Failed, Network, VS/Tower, Bond feeding and
      Summon while refactoring.
- [ ] Add structured logs for timeouts. (Bounded, privacy-safe owner-change log is implemented.)

Acceptance: existing features behave identically, but priority is expressed in one tested component.

### Phase 2 – Shared recognition/action foundation

- [x] Add an allocation-light pixel-frame abstraction with clamped access and OpenCV-scale HSV.
- [x] Add centered 9:16 game viewport fitting and normalized coordinate mapping.
- [ ] Add reusable connected components and frame hashes (patch colour ratios are available).
- [ ] Add motion/settle tracking shared by menu tasks.
- [ ] Connect the pure `VerifiedActionController` to Accessibility tap/swipe/back execution. (Source
      screen, expected result, timeout, retry budget and safe parking are implemented and tested.)
- [ ] Add safe normalized swipe support to the Accessibility service.

Acceptance: task modules no longer duplicate pixel access, cooldowns or pending-gesture recovery.

### Phase 3 – Gekkomon Run

- [x] Add initial pure detector for Fever bar plus red/magenta obstacle components.
- [x] Add Jump/Slide analyzer, two-frame stability gate, cooldown and UI toggle.
- [x] Add synthetic detector tests.
- [ ] Add real screenshots for low, tall and floating obstacles, result, pause and event screens.
- [ ] Track obstacles across frames and estimate impact time/speed.
- [ ] Detect event page, Play Game, pause/result/reward overlays and safe Quit.
- [ ] Add automatic home → Events → Gekkomon navigation and home return.
- [ ] Add Fever target, 15,000-point stop, daily reset and session summary.

Acceptance: from a clean home screen it runs until the configured Fever target, never claims rewards,
never plays past 15,000 points and returns home.

### Phase 4 – Meat Field

- [ ] Add screen detectors: Explore menu, seed dialog, water popup and home. (Conservative Meat Field
      2×3 lattice detection is implemented and synthetic-tested.)
- [ ] Detect the 2×3 plot lattice and classify empty/growing/ripe with confidence.
- [ ] Implement verified harvest and free-seed planting; explicitly reject Water.
- [ ] Read/verify free-seed count where reliable, otherwise use conservative visual proof.
- [ ] Navigate home → field and field → home.
- [ ] Persist next due time and learned grow duration.
- [ ] Add session counts: harvested, planted, skipped, unreadable.

Acceptance: all six plots are handled without spending non-free resources and the app returns home.

### Phase 5 – Bond rotation

- [ ] Split current `FeedFrameAnalyzer` into reusable home/token detector and action controller.
- [ ] Detect Digimon tab, Partner subtab, expand-grid control, partner cell lattice, raised marker,
      Raise button and confirmation dialog.
- [ ] Start a tour only after a token collection or explicit manual request.
- [ ] Iterate partners with verified Raise and home return after every selection.
- [ ] Restore the original partner even after partial failures where safely possible.
- [ ] Persist only safe resume information; never resume mid-confirmation after process death.
- [ ] Add visited/collected/failed/restored summary.

Acceptance: every recognized partner is visited once, token collection is attempted and the original
partner is restored. An unreadable grid causes a safe stop, never a blind selection.

### Phase 6 – Dungeon rotation

- [x] Separate existing VS/Tower loop from the general dungeon task.
- [ ] Detect dungeon list/card lattice at top and bottom scroll positions. (The list and both live
      positions are known; stable per-card identity/counter extraction is still required.)
- [x] Add settings model for per-dungeon attempts, individual card inclusion and optional ads.
- [ ] Read ticket/ad counters conservatively and reconcile before/after values.
- [ ] Implement card selection, party dialog, start, battle wait, result, loss and list return.
- [ ] Integrate Network Defense as a dungeon-specific strategy without weakening its strict detector.
      (Its Challenge -> incomplete-team -> Matching -> party -> reward path is live documented.)
- [ ] Add Clear Previous Difficulty threshold and safe confirmation.
- [ ] Add tickets/attempts/wins/losses/ads/skips/unknown summary.

Acceptance: selected dungeons consume no more than configured budgets and every transition is
verified. Unknown results park the task and preserve remaining budgets.

### Phase 7 – Flexible DigiWorld sprites

- [ ] Introduce `PlayerProfile` with Botamon as default.
- [ ] Add generic dark-body and eye-pair candidate detectors.
- [ ] Fuse template/colour/body/eye evidence into scored candidates.
- [ ] Add temporal motion model constrained by dispatched direction and scrolling behaviour.
- [ ] Prevent large sprites from contaminating item/obstacle scores in neighbouring cells.
- [ ] Add an in-app calibration/preview showing selected player cell and confidence.
- [ ] Test each advertised sprite profile with full screenshot sequences.

Acceptance: Botamon remains at least as reliable as today; additional profiles are advertised only
after repeatable device tests. “Any Digimon” is not claimed.

### Phase 8 – Full-auto task director and chain

- [x] Define and unit-test separate Semi Auto / Full Autopilot policy gates; default safely to Semi.
- [x] Expose and persist the mode selector in the main UI (Expedition explicitly marked in development).
- [ ] Add enabled/due/budget contract for every task.
- [ ] Add configurable task order and optional repeat.
- [ ] Require a recognized clean home before starting navigation.
- [ ] Add returning-home and parked recovery shared across tasks.
- [ ] Make background/overlay status expose current task and phase.
- [ ] Add manual “run now” per task without changing the persistent chain.

Acceptance: the app can be left on home, runs all due jobs in order and stops or repeats as configured.

### Phase 9 – Persistence, summaries and diagnostics

- [ ] Add versioned preferences/repository layer and migration from existing keys.
- [x] Add game-day reset logic at 08:00 with unit tests around DST and date boundaries.
- [ ] Store compact daily counters and last task outcome; avoid storing screenshots by default.
- [ ] Add a compact in-app “Today” checklist for all tasks with done/open/skipped/error states and
      the next 08:00 Europe/Berlin reset. Dungeon completion persistence/reset is implemented first.
- [ ] Add current-session and today summary UI.
- [ ] Add opt-in diagnostic frame export with automatic privacy warning.
- [x] Specify the privacy-safe support bundle, required metadata and error-report checklist.
- [x] Implement bounded structured event log and user-reviewed ZIP export (single UTF-8 report).

Acceptance: process restarts preserve configuration and safe counters without resuming dangerous
mid-action state.

### Phase 10 – Release hardening

- [ ] Add screenshot fixtures for supported resolutions/aspect ratios and languages.
- [ ] Add long-running state-machine simulations and gesture callback timeout tests.
- [ ] Test physical Samsung/OnePlus plus supported emulators.
- [ ] Update README, troubleshooting, release notes and feature safety descriptions.
- [ ] Produce signed beta, gather logs/screenshots, tune detectors, then promote individually.

## 8. Recommended implementation order

Do not implement all navigation independently. The efficient order is:

1. frame ownership;
2. shared pixel/geometry/action primitives;
3. finish Gekkomon detector and collect fixtures;
4. Meat Field (smallest complete home-to-home job);
5. Bond rotation (reuses home navigation and verification);
6. Dungeon rotation (largest menu/task state machine);
7. multi-sprite DigiWorld;
8. full-auto chain and summaries;
9. hardening and release.

## 9. Testing matrix

Each detector should be tested against positive, adjacent-negative and unstable frames.

| Area | Positive fixtures | Required negatives |
| --- | --- | --- |
| Home | EN/DE, food bubble/no bubble | battle, partner page, modal |
| Farm | empty/growing/ripe, seed dialog | water popup, unrelated grid |
| Bond | partner grid/raised marker/Raise | other Digimon tabs, dimmed UI |
| Dungeon | list/cards/party/result/loss | summon dialogs, Tower lookalikes |
| Runner | low/tall/floating obstacle | event menu, ordinary battles |
| DigiWorld | every supported sprite | items, claws, dark obstacles |

For every action path test: success, delayed transition, gesture callback missing, source screen
disappears, unknown modal, Stop pressed and capture interrupted.

## 10. Known blockers and evidence needed

Screenshots improve validation but are NOT required from the user before further implementation.
Reference thresholds and state transitions should be reconstructed directly where readable.

- The Gradle wrapper/build baseline is restored; use Android Studio's bundled JBR as documented in
  section 11 on this Windows machine.
- Real screenshot/video sequences are needed for Farm, Partner rotation and Gekkomon timing. The
  decompiled app contains thresholds and templates but not enough representative full frames.
- Large-sprite support cannot be promised without captures of each intended Digimon while standing
  in several rows/columns and while the board scrolls.
- The reference APK is decompiled output. Some complex methods are incomplete in JADX; behaviour must
  be inferred from state transitions and verified independently.

## 11. Current implementation notes

### 2026-09-24 — Integration into v4.0.1-beta.3

- Leading codebase is `.release-v3.2-beta1` at tag `v4.0.1-beta.3` (versionCode 66).
- Merged the Fusion packages `automation`, `farm`, `runner`, `support`, and `vision`,
  plus `DungeonBudget`, their JVM tests, and the support/automation hand-off documents.
- Integrated Fusion changes into `MainActivity`, `ScreenCaptureService`, `AutomationState`,
  and EN/DE strings while retaining the v4.0.1-beta.3 target as the leading source.
- Preserved the target Gradle/version configuration; the older Fusion versionCode/versionName and
  debug application-ID changes were not applied.
- Kotlin production sources compile successfully. The Android SDK build-tools 36.0.0 AAPT2 binary
  was used as an explicit override because the Maven-cached AAPT2 executable cannot start under the
  local Windows policy. `assembleDebug` completed and the JVM suite passes: 139 tests, 0 failures.
- Home -> Explore -> Meat Field -> Home remains recognition/controller groundwork only; automatic
  navigation is not yet live and must not be presented as completed.
- First BlueStacks live test exposed unsafe prototype gates: Runner tapped at confidence values as
  low as 0.13, Farm displayed its status without owning the frame, and Dungeon could read beyond a
  closing image buffer. Automation was stopped immediately. Runner now requires three active frames
  and confidence >= 0.75, Farm dialogs are accepted only inside a flow that began on a positively
  recognized field, unrelated screens no longer receive Farm status, and Runner/Dungeon validate
  image dimensions and buffer bounds. The corrected signed build passes 139 tests and was installed
  over the existing v4.0.0 on BlueStacks with the matching release certificate. These changes are
  safety containment; real Gekkomon/Farm success still requires fixture-driven live validation.

### 2026-09-24 — Live DigiAutoTap/BlueStacks screen audit

- Confirmed the reference app is installed separately as
  `io.github.digipr1me.digiautotap` and supplies the compact top overlay shown by the user.
- Captured the current 720x1280 game Home screen and Explore menu. The reference overlay reported
  `Parked · explore_menu` on the Explore page and `Parked · dungeon_list` on the dungeon list.
- On the World Search grid it switched to `Watching` with a green status indicator. This confirms
  that the reference first classifies the open activity, then changes director state; it does not
  expose raw task-detector matches as successful actions.
- The reference main screen distinguishes service state (`STOPPED`, `IDLE`, `PARKED`), mode,
  task availability and today's result. Its semi-automatic Meat Field description explicitly says
  the field must already be open and remains open afterwards. Gekkomon semi-automatic mode starts
  from the event page containing Play Game and Missions, runs to the configured Fever target and
  leaves the page open.
- Meat Field, Dungeons and Gekkomon are locked in the installed reference build without its
  supporter code, so live execution cannot be used as the sole oracle. Decompiled state transitions
  plus independently captured game screens remain necessary.
- Architectural consequence: replace independent analyzer status/tap decisions with a global,
  stable `ScreenClassifier` result and a director-owned overlay snapshot. Only the module assigned
  to that classified screen may observe/act; unknown or conflicting classification parks globally.

### 2026-09-23

- Repository `main` fetched from `RobinTh0r/DigiWorldExplorer_Android_Bot`.
- Decompiled `DigiautoTap/` inspected as reference and intentionally left untracked.
- Confirmed full-auto home navigation, Farm, Bond tour, dungeon budgets/rotation, Gekkomon runner,
  task chain and selected daily persistence in the reference.
- Added initial `GekkomonRunScreenDetector` and `GekkomonRunFrameAnalyzer`.
- Added UI preference/toggle and EN/DE help/status strings for the runner.
- Added synthetic tests for no-Fever rejection, jump, slide and wait-for-distant-obstacle cases.
- Added a central `FrameOrchestrator`, explicit `FrameOwner` values and priority/short-circuit tests;
  migrated the capture analyzer chain to ordered probes.
- Added the Gradle 9.4.1 wrapper required by Android Gradle Plugin 9.2.0.
- Build command on this Windows machine:
  `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat test --no-daemon`
- Test baseline after runner/orchestrator work: 19 suites, 96 tests, 0 failures/errors/skips.
- Generated the ignored local debug keystore and successfully built
  `app/build/outputs/apk/debug/app-debug.apk` with `assembleDebug`.

### 2026-09-24

- Added the explicit `AutomationMode` model and pure policy gate. Semi Auto cannot navigate; Full
  Autopilot navigation requires a positively recognized clean Home screen.
- Added unit coverage for safe preference fallback and navigation rules.
- Added `docs/SUPPORT_AND_DIAGNOSTICS.md` with support-bundle contents, privacy exclusions and the
  error-report hand-off checklist.
- Added shared `PixelFrame`, RGB→OpenCV-HSV conversion, centered 9:16 viewport mapping and normalized
  patch sampling with JVM tests.
- Added conservative Meat Field lattice detection and synthetic positive/missing-plot negatives. It
  is recognition-only and deliberately cannot tap before real fixtures cover all plot/dialog states.
- Added a bounded 200-entry, non-sensitive automation event ring and capture/frame-owner logging.
- Added the pure verified-action lifecycle: only allowed source screens dispatch, expected screens
  complete, timeouts retry within budget and then park safely.
- Added the 08:00 local `DailyResetClock`, including pre-reset and Europe/Berlin DST coverage.
- Final fresh isolated verification (avoiding a Windows/OneDrive lock in the workspace build
  directory): 25 suites, 113 tests, 0 failures; `test assembleDebug` succeeded. The verified local
  test APK is copied to `artifacts/DigiWorldExplorer-beta-debug.apk` (ignored by Git).
- BlueStacks install hardening: debug builds use the separate
  `de.robinthor.digiworldexplorer.beta` application ID. They can coexist with a differently signed
  installed release instead of failing with an update-signature conflict or requiring data loss.
- Corrected the obsolete blocker note: wrapper/test/build baseline is now available as documented
  above; real device screenshot sequences remain the main recognition blocker.

### Active work — Co-Pilot / Expedition expansion

Implementation status (latest verification below):

- Renamed visible modes; original enum/preference IDs retained. Expedition help explicitly says
  automatic navigation is not connected yet, rather than promising a working end-to-end tour.
- Fixed unsafe VerifiedAction retries on unknown screens/unexpected dialogs; source recognition is
  now required again at timeout. Added regression coverage.
- Support export is connected to the main UI: preview, Android Save Document, ZIP, explicit metadata
  allowlist, last 200 events. No raw preference export. Device UI testing remains outstanding.
- FarmController implements single-dispatch harvest, free-seed selection, selection proof, counter
  reconciliation, Water close/park, bubble-covered plot skipping, timeout and cancellation. The
  controller now retains the last proven counter while the seed dialog covers that HUD value.
- FarmDialogDetector reconstructs seed-slot/select-button/selection-bracket and Water-popup rules
  with OpenCV-scale HSV and eight-connected components, requiring partial field context.
- FarmHarvestDetector uses six distinct soil components plus nearby badge/number geometry. A flat
  orange frame cannot match. Missing or ambiguous glyphs remain UNKNOWN.
- FarmHarvestAnalyzer IS wired into capture as opt-in "Meat Field: Farmhelfer (Beta)". It now drives
  the complete manually-opened field visit: harvest, open seed menu, select the first free slot,
  confirm, verify GROWING and reconcile an exact counter decrement. Water is closed and causes a safe
  park. Every observation must match twice, actions time out, bubble-covered targets are skipped and
  gesture rejection parks the flow. Stop/toggle/capture release reset pending work. It still does NOT
  navigate to/from the field. No real-device success is claimed yet.
- TaskDirector implements ordered due/budget selection, Co-Pilot manual activity ownership,
  Expedition navigation/home-return handshakes, repeat scheduling and bounded navigation. Pure
  workflow only; adapters must supply truthful screen/task identities and refresh due/budget state.
- DungeonBudgetLedger reserves attempts before dispatch, rejects duplicate starts/results, counts
  unknown stopped attempts conservatively, and defaults ad spending to zero. Not yet wired to UI.
- Reference inventory also contains QuestSkill, PassiveSkill, TowerSkill, SummonSkill, WorldSearchSkill,
  RunnerSkill, FarmSkill, DungeonSkill, BondTourSkill and LogSkill. Quest progress/stage handling,
  passive hologram/partner actions and their exact settings need an expanded parity audit.

Next concrete steps:
1. Validate the manually-opened harvest/plant beta on device; fix viewport/font differences from
   diagnostics while retaining UNKNOWN-on-uncertainty behaviour.
2. Add verified home -> Explore -> Meat Field navigation and a bounded verified return-home route.
3. Extract the Farm gesture bridge into unified action dispatch/callback ownership for other routes.
4. Bond grid identities/restoration, dungeon rotation/counters, runner timing and daily targets.
5. Complete Quest/Passive/Tower/Summon parity audit; never mark a pure controller as a live feature.
6. Latest clean verification after live harvest integration: `test assembleDebug` SUCCESSFUL,
   31 suites / 130 tests / zero failures. APK refreshed at `artifacts/DigiWorldExplorer-beta-debug.apk`.
   Test builds run outside OneDrive in a fresh temporary source copy excluding generated build data.

Concrete recognition continuation notes from reference inspection:
- Farm free-seed counter ROI is y=.06.. .11, x=.375.. .455 in the game viewport. Grayscale bright
  mask threshold starts at 200; components cluster by row, retain glyphs >=85% of row height;
  accept one or two digits only when every digit is known. Unknown must not become zero or free.
- FreeSeedCounterReader and ShapeDigitReader implement that ROI, component-row selection and
  reference-derived 12x16 hole/density rules. They are connected to planting; a missing/uncertain
  value parks rather than becoming zero. Tests cover two glyphs, excess glyph rejection, empty/noise
  and core digit topology. Synthetic 1 glyphs include their baseline so their tight crop is realistic.
- PlotTimerReader reconstructs the bright timer ROI per plot, paired colon dots, four/six-digit row,
  shape digits and valid MM:SS / HH:MM:SS bounds. A successful read upgrades an otherwise badge-free
  plot from UNKNOWN to GROWING, enabling visual planting verification later.
- Existing HudCounterReader has only 1/2/3/5 templates in a different HUD-specific outline font;
  do not assume those templates cover the Farm counter. Inspect the reference digit recognizer.
- Badge area >=.0012 of viewport, HSV H30..45/S180..255/V20..140. Badge numbers need at least two
  similarly sized, adjacent digit-shaped components. Live beta is stricter for EMPTY (no bright ink).
- Cyan bubble veto is now ported into live harvest and planting authorization: HSV H95..112/S70+/V180+,
  connected-area threshold scaled to the normalized detector grid and radius .06 around the inward
  plot target. A ripe plot remains recognized but is skipped while its tap point is obstructed.

Build policy for the current implementation pass: do not run Gradle after every small edit. Use
source/diff checks while batching features, then run the clean external `test assembleDebug` gate and
refresh the beta APK at the next major milestone. Therefore the APK named above predates the newest
timer/counter/full manual Farm changes until that gate is explicitly recorded again.

Explore navigation recognition now has a first pure adapter, `ExploreMenuDetector`. It requires at
least 45% dark-cyan menu body in y=.16.. .86, then a dominant magenta World Search anchor near
(.294,.214), and only accepts the green Meat Field card when its tap point is offset from World
Search by dx=.0715/dy=.2442 (tolerance .03). The corresponding HSV/component thresholds, dominance
ratio and synthetic geometry test are recorded in source. It is intentionally not allowed to tap
until clean-home/Explore-tab recognition and transition verification are implemented.

Known limits deliberately retained: centered 9:16 fitting is a geometry assumption, not automatic
letterbox detection; the old patch-only MeatFieldScreenDetector is not used to authorize harvesting.
APK package separation avoids possible signature conflicts; the earlier BlueStacks error did not
prove a signature mismatch, and the reachable device reported API 30 rather than Pie/API 28.

### BlueStacks live screen audit — 2026-09-24

- Audited the installed game at 720x1280 / Android 11 through Home, Explore, Digital World Search,
  Dimensionsbox, Meat Field, Training, Food, Lexicon, Partner, Camp, Dungeon list/detail, Missions,
  Summon, Events and the Gekkomon event/minigame. These are separate Director screen identities;
  they must not be inferred only from a task toggle or a single shared colour patch.
- The real Meat Field supplied fixtures for FIELD/RIPE, FIELD/EMPTY, SEEDS, FIELD/GROWING with a
  water bubble, and WATER. One ripe plot was harvested and one free common seed was planted manually
  to obtain the full transition sequence. Water was not consumed. The WATER screen has a green
  `Gießen` action and no in-dialog close icon; Android Back safely closes it.
- Gekkomon `Minispiel` starts the live round immediately, without a separate confirmation. Pause
  opens a record dialog whose `Abbrechen` action returns to the event page. Consequently a runner
  false positive is inherently destructive and requires stable multi-frame ownership before taps.
- The reference app exposed useful independent screen labels in its overlay (`main`, `explore_menu`,
  `partner_page`, `dungeon_list`, `prompt`) and distinguished Watching/Parked/Unknown. Camp was still
  Unknown in the observed reference build, so parity must not be equated with copying its classifier.
- The installed v4.0.1-beta.3 capture and accessibility services started successfully. With Farm
  enabled, the fixed ownership gate remained silent on a dungeon detail for multiple frames: the old
  global `Farm prüfen` false ownership was not reproduced. On the real Meat Field, however, the
  installed Farm beta did not claim or act during a 12-second observation despite ripe plots. This
  is the current live blocker; synthetic detector success is not sufficient. Add structured detector
  diagnostics/fixture replay before changing action thresholds.
- All automation was stopped after the audit. No summon, dungeon challenge, premium currency or
  water action was executed. The only deliberate resource change was one free common seed (20 -> 19).

### Public DigiAutotap source audit — 2026-09-24

- The newly published repository at `https://github.com/digiPr1me/DigiAutotap` was inspected at
  commit `7bc044d77b4c12330f9b8c53fd18516379e6ca81`. It replaces the decompiled APK as the primary
  behavioural/parity reference. Its restrictive licence means no source is copied or redistributed;
  DigiWorldExplorer remains an independent implementation using its own fixtures and tests.
- The public source confirms that a complete screen taxonomy, ordered first-match classification,
  an idle/takeover gate, explicit skill ownership and at-most-one action per observed frame are the
  core architecture. Detailed gaps and the independent implementation order are recorded in
  `docs/DIGIAUTOTAP_PUBLIC_PARITY_AUDIT.md`.
- The live Director overlay now remains visible on Unknown, recognizes the changing Home stage and
  Explore menu, and no longer mistakes ordinary HUD bars for Gekkomon Run. Gekkomon is hidden from
  settings and removed from passive classification; it is deliberately the final task milestone.

## 12. Definition of done

### Recognition-first execution order — current master plan

1. Complete one central, ordered screen registry for Home, blocking prompts, Explore, Meat Field and
   its dialogs, Dungeon list/detail, Partner page, Summon and the remaining feature pages. Recognition
   stays read-only and must survive Director debounce before it can unlock actions.
2. Give every automation an explicit set of allowed source screens and verified destination screens.
   A task may issue at most one action per frame; UNKNOWN, conflicting evidence and capture loss park it.
3. Finish and fixture-test Home -> Explore -> Meat Field -> Home first, including seed/water dialogs,
   because Farm is the current live blocker. Then add Dungeon, Partner/Bond and Summon in that order.
4. After recognition and core routes are stable, make the compact Director overlay draggable, add an
   in-app show/hide control and reduce general button size without weakening its visible safety state.
5. Keep Gekkomon hidden and disabled until all other routes pass live BlueStacks runs. It is the final
   feature because a false positive starts gameplay immediately and is therefore high-risk.

The public DigiAutotap project remains a behavioural checklist only. DigiWorldExplorer uses its own
screen geometry, state models, tests and action gates; source code is not copied from that project.

Live validation of this registry on 2026-09-24 confirms stable overlay identities for `Meat Field`,
`Explore menu`, `Dungeon list` and `Partner page` on the running 720x1280 BlueStacks game. These
checks were passive: no battle, purchase, summon or resource-consuming action was dispatched. The
release gate now passes 147 unit tests before the signed APK is installed.

Farm live follow-up: the real field may visually merge two neighbouring soil regions, so field
recognition now requires all six plot anchors plus at least four independent soil components instead
of six components. Locked plots are classified and ignored, green timer/progress bars provide a safe
`GROWING` fallback when timer glyph OCR is incomplete, and the Farm preference is restored through
both normal and capture-consent start paths. The installed live build now reports `Active / Meat
Field / Farm: nothing ready` for the observed five-growing-plus-one-locked layout instead of parking.

The next live fixture exposed the actual harvest bubble (`x1.075`) and the visually similar empty
shovel bubble. They are now distinguished by their interior colour structure. The installed workflow
harvested exactly 1,075 meat (32,720 -> 33,795), identified the resulting empty plot, selected one
free common seed and verified the new timer (seed count 20 -> 19). The Director UI is now a compact
two-line card attached to the draggable round quick-control bubble; both drag together, and Farm is
available in its quick menu. Moving the overlay across recognition-critical game content deliberately
parks recognition on UNKNOWN rather than allowing an obscured-frame action.

The subsequent all-ripe live pass covered all three observed reward labels (`x107`, `x258` and
`x1.075`). Yield bubbles are now identified by both their meat-colour structure and the dark amount
line, while shovel bubbles remain EMPTY. The installed build harvested the remaining `x1.075` and
`x107` rewards (`34,418 -> 35,600`), then planted every verified empty plot (`18 -> 14` common
seeds). Four-frame post-action/dialog settling prevents animation frames from being treated as final;
an occluded UNKNOWN plot is skipped without blocking a separately verified neighbour. Live `15`
being misread as `12` also established that a visible growing timer is stronger planting proof than
exact counter OCR. The source tree now carries a proprietary `LICENSE.txt`; DigiAutotap remains a
behavioural reference because its published license prohibits redistribution of modified or
unmodified source/builds.

Farm resource follow-up: seed selection now uses right/red -> middle/good -> left/common priority,
but only from the large counters inside the verified seed dialog; the compact HUD is not trusted for
premium selection. Watering is opt-in and prioritises purple Palmon, red Palmon, then Mini Palmon.
Both watering dialogs and their green confirmation buttons are verified before tapping. Live testing
confirmed purple-target selection, both dialog transitions, time reduction, the resulting harvest,
and replanting. Ad-labelled watering is permitted only when the persisted global `Ad Skip Pass`
setting is enabled; that setting is enabled on the current BlueStacks installation.

The “ultimate” implementation is done only when:

- all enabled tasks can run from and return to a recognized home screen;
- no two analyzers can act on the same frame;
- every navigation/action transition is visually verified and bounded;
- Farm, Bond rotation, Dungeon rotation and Gekkomon Run have real fixture coverage;
- Botamon reliability does not regress and every additional advertised sprite has test evidence;
- stop/capture-loss/unknown-screen behaviour is safe;
- settings, daily counters and task summaries survive process restart;
- a repeatable Gradle command passes all tests and builds the beta APK;
- README and in-app help accurately describe limitations and resource-spending behaviour.
# Dungeon rotation live evidence (2026-09-25)

- Added an explicit `Start Dungeon Rotation` overlay action. It is separate from the existing
  `VS / Tower Loop`, which continues to operate only after a supported challenge has started.
- Live-verified the guarded Home -> Dungeon-list transition on BlueStacks. Both source and result
  require two matching frames; this test opened no card and spent no ticket.
- Current live card order: Apocalymon Wall, DemiDevimon, Bakemon, Digifactory, Network Defense
  Ops, Metal Sea, and VS Defense-type Digimon.
- Live counters showed `2/2` on the five regular cards. Apocalymon showed material stock/cost
  (`51/2`), not a daily attempt counter, so its policy is capped at one successful run per pass.
- The VS card opens a distinct `Destroy` panel and currently showed zero of its required red
  resource. It must be entered once only and then handed to the existing VS automation module.
- Regular ceilings are four starts (two normal plus at most two ad starts). Ad starts are gated by
  the global Ad Skip Pass switch. A visible zero or an unreadable counter never grants spending.
- Before card execution is enabled, scrolling must be visually proven at the requested end and
  card counters must be read stably. A swallowed swipe must park that half of the pass rather than
  shift every card index.
- Live completion pass: Apocalymon, DemiDevimon, Bakemon, Digifactory, Network Defense Ops and
  Metal Sea each completed once and produced their reward screen. Every regular counter changed
  from `2/2` to `1/2`; Apocalymon material changed from `51/2` to `50/2`.
- Network Defense requires a distinct flow: Challenge can report an incomplete team; dismissing
  that notice and selecting Matching formed a three-player party before the battle.
- The VS card opened correctly but showed zero destroy currency. Destroy was not pressed, proving
  that a zero-resource panel must end the card safely instead of being treated as a free action.
- Bond/Farm analyzers are now suppressed while a verified dungeon session is active. Live battles
  had otherwise produced false `Partner / Bond` and `Explore menu` overlay labels.
