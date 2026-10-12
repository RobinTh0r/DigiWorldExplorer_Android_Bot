# Local diagnostic/DWS follow-up — 12 October 2026

Unpublished working-tree changes based on b35e4e8 / public v5.5.1-beta.1.
No version increment, push, tag, release, device installation or live run in this follow-up.
App ID, signing configuration and saved settings/data remain unchanged.

## What the S23 evidence actually establishes

The supplied Downloads/DigiWorldExplorer-diagnostics-all.zip contains a 5.5.0/code86
run on SM-S918U, Android16, 1440×3088, density560, fontScale0.8, Classic V4_DASH.
40 delivered board taps and saved images show movement, but no Dash-button tap.
The log contains a one-row grid-phase revocation (926→719 top), and a brief startup
crop with left469 before returning to full display. This is not evidence of a new
physical-phone live test, nor sufficient to attribute every pause to that geometry.

Actual unchanged native screenshots show stock1,148 and1,139. Before the change,
the reader returned null. Its number ROI was narrow, and the outlined thousands
comma touched the first digit. Replaying a 540px JPEG at a larger size cannot restore
those original details; those replays still return unknown and are not a phone test.

The shared reader now uses the measured plate's whole number area and separates
lower punctuation from the number body, preserving the digit serif. Detached
punctuation cannot increase digit count. A genuine observed outline-1 variant is
included (not the combined digit/comma). Both native evidence images now prove a
minimum1000; unknown suffixes are NOT falsely reported as an exact1148/1139.
Unknown/zero/missing stocks remain conservative; no model/resolution branches added.
Native test fixture covers the HUD only as recognition evidence: its drawn debug
grid must not be used to prove grid detection.

## Calibration safeguards

An initially narrowed game-content rectangle must have three continuous seconds
of proof before mapping can be calibrated. No guessed full-window mapping is
authorized during that wait. A fully rendered window can establish normally;
real window/display/surface/density changes restart proof and revoke old mappings.

A mismatching clean DWS recheck immediately blocks input. One bad animated row
phase no longer erases all navigation state; three consistent changed/missing
observations revoke calibration. Alternating unconfirmed phases never authorize
input. Pending burst callbacks are invalidated by an epoch on pause/reset, so they
cannot resume after a later good frame. This is temporal safety, not a proof that
GridDetector can never choose the wrong phase on any game scene.

## Diagnostic schema2, opt-in

- Existing device/settings/current geometry context retained, excluding activation codes.
- `dws-current.json`, `dws-trace.jsonl` (+ `.previous`): frame/generation IDs,
  actual analysis/capture/display mapping, grid bounds/confidence, all25 cell scores,
  preview, detected HUD layout/stocks/unknown glyphs/button, motion delta and wait
  reasons, selected player/plan, intended analysis/display taps, and resource outcomes.
- DWS JPEGs are the actual pre-decision frame, not a later next-frame screenshot.
  Sampling is approximately8s, with extra mismatching-grid/no-progress evidence.
- `native-hud/*.png`: lossless original-pixel resource area, accompanied by its
  rectangle in the analysis frame. No JPEG/resizing of the number outlines.
- `action-frames.jsonl` (+ `.previous`): coordinate kind (canonical analysis versus
  unconfirmed raw capture), source/image dimensions, geometry generation and sampled
  action metadata. `native-target/*.png` supplies original-pixel tap neighborhoods
  for general features, including Bond and Dungeon, from sampled next/followup frames.
  These crops are omitted when geometry changed/is unconfirmed. They are not pre-tap.
- Gesture dispatch/completion carries the sampled evidence ID and coordinates;
  rejected taps distinguish missing/stale geometry, outside-game and overlay coverage.
- `coverage.json` at export lists the actual included files, sizes and evidence limits.
  README explains missing/evicted evidence and the difference between input delivery,
  a heuristic visual change, and successful game acceptance.

Retention is rolling rather than first50-only: up to50 compact screenshots, up to12
native HUD and12 native target crops per session. Approximate category byte budgets
are12MB/8MB/8MB; newest oversized evidence is preserved by policy. Events are two
2MB parts, DWS trace two1MB parts, action trace two300KB parts. Six sessions retained
as previously. Trace references to evicted images can remain; coverage manifest is
the source of truth for available files. Sampling means no complete video or
guarantee that every transient UI is present. Screenshots may contain personal
information: review before sharing. New evidence and the legacy debug PNG export
are gated by Diagnose; no background image writing with Diagnose off.

The richer decision trace is currently DWS-specific. Other features get the shared
gesture/image improvements and their existing status/recognition events, not an
equivalent full internal-decision trace. Do not claim universal 1000% fixability.

## Verification / next

739 unit tests in85 suites, zero failures/errors/skips after the implementation;
includes original native stock, modified separator/erased stock negatives, startup
partial-frame/letterbox/density-change, grid recheck and rolling-retention tests.
The existing synthetic projection matrix includes1080×2340,1080×2400 and six other
sizes, navigation and capture scales; it is not live handset verification.
Debug APK build and full debug lint succeeded (0errors,155warnings,1hint).
Full lint also found two pre-existing errors: misplaced manifest localeConfig text
and Compose support-export configuration access. Both are corrected, not suppressed.

Release build/signature final verification is recorded in HANDOFF_5.5.1_BETA1.md.
No phone/BlueStacks/live ZIP export validation in this follow-up. Next: inspect a
new opt-in session and exported ZIP from the updated app, including long-run retention,
native crop mapping, blocked recheck and an actual Dash/resource follow-up. Keep USB
debugging OFF for phone gameplay (00000038). A BlueStacks result would not prove S23
compatibility. Before any distribution, choose an explicitly authorized new version;
do not replace public beta.1 with this local same-version candidate.
