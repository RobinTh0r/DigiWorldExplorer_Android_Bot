# DigiAutotap public-source parity audit
Reference inspected: `https://github.com/digiPr1me/DigiAutotap`, main commit
`7bc044d77b4c12330f9b8c53fd18516379e6ca81` (2026-09-24).

The repository is a much stronger behavioural reference than the decompiled APK. It exposes the
original module boundaries, state vocabulary and tests. Its licence prohibits redistribution of
source or modified builds, so DigiWorldExplorer must not copy its implementation. We use it only to
identify observable behaviour and independently implement/test equivalent workflows.

## Architectural gaps found

- Its Director classifies exactly one screen before assigning work. The published screen taxonomy
  includes Home, Stage Failed, insufficient tickets, exit/pink prompts, idle reward claim, partner
  window, title, dungeon list, summon and summon dialog, Explore menu, World Search board, Meat
  Field and field dialog, partner page, event page, generic dialog and Unknown.
- Classification order is a safety rule. Blocking banners/prompts and strong Home evidence precede
  task pages; generic dialogs and Unknown are last. Multiple independent anchors are required for
  ambiguous pages (for example, Meat Field uses plot geometry plus the white close button).
- Screen recognition, motion/takeover gating, task selection and task execution are separate layers.
  The current DigiWorldExplorer implementation still mixes recognition and actions in several frame
  analyzers. `PassiveScreenClassifier` is the first read-only separation and must become the only
  source of Director screen identity.
- The reference waits for a stable/idle screen before taking control and permits at most one action
  from a classified frame. It also distinguishes a task's expected transition from an unrelated
  prompt and parks when ownership cannot be proven.
- Tasks implement explicit `works on`, `has budget`, `sees work`, semi-automatic work and full-run
  responsibilities. DigiWorldExplorer's pure `TaskDirector` has part of this model but lacks live
  adapters for most tasks.
- The public tests cover classification oracles, route planning, task budgets, Farm live flows,
  Dungeons, Quest, Summon, Bond, Passive actions, World Search and Runner. The undistributed image
  corpus/templates mean its full oracle cannot be reused; our own BlueStacks fixtures are required.

## Independent implementation order

1. Complete read-only screen taxonomy and stable transitions: Home, prompts/title/rewards, Explore,
   World Search, Meat Field/dialogs, Dungeon list/dialog, Summon/dialog and Partner pages.
2. Add motion/idle ownership gating and one-action-per-observation enforcement above every task.
3. Finish Farm and Home -> Explore -> Farm -> Home using our own live fixtures.
4. Add Passive/Bond and Quest rounds on the confirmed Home screen.
5. Add Dungeon budgets/rotation, then Summon modes and World Search integration.
6. Add session/daily persistence and full Expedition chaining.
7. Gekkomon Run last. Its UI entry is hidden and its passive classification disabled until then.

## Overlay follow-up

- Keep the compact Screen/Action card.
- Add drag positioning with saved coordinates.
- Add a normal in-app show/hide switch independent of the debug grid.
- Reduce the quick-control and settings buttons after recognition work is stable.
