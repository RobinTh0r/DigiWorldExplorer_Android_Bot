# Global Growth Guide recovery — 12 October 2026

Unpublished follow-up on this authoritative worktree; prior diagnostic/DWS changes
are preserved. No version increment, publication, installation, emulator or phone run.

## Evidence and historical difference

User supplied diagnostic-20261011-155421.zip and a native1080×2340 screenshot.
The ZIP is5.5.0/code86 on SM-S931B/Android16, density420, fontScale1.15, en-AU.
Both saved540×1170 screenshots show Dungeon Failed/Growth Guide. Logs show
`Dungeon screen not confirmed`, PARK and no gesture dispatch in the recorded window.
Capture briefly changed to695×1370 (origin192,333), then full1080×2340 again.
The ZIP does not contain the earlier start/current-key state, so it cannot establish
the exact selected-key value when the error began or the second reporter's device.

Verified v4.0.0 source checked StageFailed centrally before per-mode dispatch.
In v5.0.0 it was inside the ordinary probe list, bypassed by exclusive Dungeon
rotation. Source history associates that exclusive branch with5c3cc5a (5.0.0 beta2);
later exclusive sessions (e.g. Bond) also bypassed ordinary probes. Current beta.1
had an owned loss fallback but required an already selected non-Network dungeon.
Thus a recognizable guide can be stranded outside that narrow owned-battle path.
This is an ownership/dispatch gap, not proof that the global detector was deleted.

## Changes

- Specific foreground recovery runs before all exclusive sessions, Summon/Entry,
  Dungeon rotation and ordinary mode probes. Capture-quality and foreground-game
  guards still run first. Disabled automation never dispatches a recovery tap.
- Shared pure detector retains red failure-title + medium-gray header + red guide
  content proof, additionally measures the actual wide header. No OCR language,
  device-name or resolution profile. Early negative exits limit normal-frame cost.
- Targets are observed dark left/right backdrop beside the measured header's extent,
  not fixed .50/.84 or the Fight/Skills/Digimon shortcuts. Overlay-obstructed targets
  are excluded; no proven safe target means no tap.
- First positive frame blocks competing input; two fresh scans at500ms cadence
  authorize dismissal. Old queued taps/swipes/Back from other modes are rejected
  while guide recovery owns input. This is safety: an already queued action rejected
  by its caller may park that caller and require a restart; no claim of universal
  transparent resumption from every interrupted transaction.
- At most3 delivered/uncertain dismissal attempts per continuously visible guide,
  spaced1.5s. Rejected input does not consume an attempt; missing callbacks time out
  conservatively. Tokens/epochs exclude late callbacks from newer input state.
  No automatic game setting/account-resource changes and no Android Back dismissal.
- A fresh absent-guide observation releases ownership. Android input completion is
  not reported as successful dismissal. Blocked recovery has English/German status.
- Rotation loss accounting still uses once-per-start BattleEvidence: stale startup
  guides cannot invent a loss; repeated positive frames cannot inflate attempts.
  Unknown-screen timers are cleared while the guide is being handled.
- Opt-in `GROWTH_GUIDE.*` logs record recognition scores, measured header/targets,
  missing target/overlay block, input outcomes/timeouts and observed disappearance.

## Actually tested

750 tests in87 suites pass, zero failures/errors/skips. New positives: both supplied
images and existing bright/dimmed S25 failures. Ten actual negative fixtures cover
announcement, combat/card flash, Network dialogs/boss, Partner, summon and reward.
Eight synthetic image variants cover1080×2340,1080×2400,720×1612,1440×3088, each
with/without48px black margins, asserting target offsets and outside-panel position.
Pure tests cover global precedence versus every exclusive owner, confirmation,
disappearance, cadence, bounded attempts and rejected/stale observations.

Signed release build/vital lint and full debug lint pass (0errors,155warnings,1hint).
APK app-release.apk25,812,244bytes SHA256
ea28faeab395e5209917e3462ee7df19bb361c7368dfe4fdfeab4fe49decc20b;
original signer verified unchanged. Still code87/
name5.5.1-beta.1 for local verification ONLY; do not distribute under the public tag.
The original published named APK/artifacts are preserved.

These are source, real-image replay and synthetic/state tests, not a successful live
Growth Guide dismissal. Next: verify that the observed backdrop actually closes this
guide in-game, fresh absence resumes the intended mode, loss recorded once and no
shortcut/exit/card taps. Physical-phone USB debugging OFF; use in-app Diagnose.
Original ZIP/private evidence remains outside the repository; only two images are
regression fixtures. No new release without explicit user authorization.
