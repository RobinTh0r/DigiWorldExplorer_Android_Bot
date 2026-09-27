# Bond collection comparison — 2026-09-27

## Revised user timing

User requested faster behavior after testing: detected tap settles for 1.5 seconds;
no detection waits six seconds before the bounded fallback sweep, then settles for
1.5 seconds. Fallback completion is separate from collection evidence. Fixed stale
previous-partner evidence being passed into the next HOME-to-COLLECT transition.
The ten-second verification implementation described below is superseded.

Reference: digiPr1me/DigiAutotap main, commit eb65b2587bf5d9158bdcb7490b89c6a4a868ca1f.
Files: core/src/main/kotlin/io/github/digipr1me/digiautotap/core/BondTour.kt and PassiveSkill.kt.

Reference behavior: wait up to 25 seconds for a bubble; allow a further 20 seconds after
sighting; tap immediately; retry up to three times at four-second intervals; observe after
the tap for ten seconds so a respawn disappearance is not confused with collection.
The reference aims at the character below/left of the detected bubble.

Confirmed local defects fixed:
- The five blind fallback taps incorrectly recorded a successful collection without evidence.
- A detected bubble only armed a tap for the following frame, where disappearance cancelled it.
- Collection waited behind three matching screen classifications.

Local change: fallback never records success; detected bubbles are tapped on the same frame;
rotation collection retries are bounded and require ten seconds without a new sighting after
a detected tap. First sighting extends the observation deadline by at least twenty seconds.
Collection taps require currently recognized Home. Unit tests cover false success without a
tap, respawn reappearance, retry limits and reset for the next partner. Tests/build passed.

Still to verify live: bubble-versus-character tap target on additional stages.
No change to the tap target yet. Header occlusion by the overlay remains a separate open issue.
The revised Bond timing is included in Beta 3.
