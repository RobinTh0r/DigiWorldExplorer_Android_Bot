# DigiWorldExplorer 5.3.0

## DWS: clearer modes and restored green Dash use

- **Classic / Bota Walk** is the free default, using the existing Botamon movement, energy and Dash rules. Those rules were not redesigned in this release.
- **All-Sprite Test · Beta** remains a separate experimental option with its existing Beta access requirement.
- V3 is hidden from the chooser. Previously saved V3 settings are preserved until the user selects another mode; existing activation codes and app settings are retained.
- Updated green Dash stock can be read even when outlined digits touch. Confirmed examples include 271 and 270. If only a positive multi-digit prefix is recognized, the bot may use an explicitly recorded conservative minimum, not invent the exact number. Unknown/zero leading stock remains blocked.
- Dash targets the lower-left green action, never the upper-right broom. Reward-strip edges are rejected as board rows. Quick status refreshes after movement resumes instead of retaining a stale no-grid pause.

## Dungeon rotation and diagnostics

- Fix first-start capture timing and fast Ad-Skip ticket rewards. An unconfirmed Ad tap can no longer retry by pressing the Challenge/Matching button.
- Add bounded Network retained-team recovery to inspect remaining ad attempts. Locate its visible counter and ad button across the tested short/tall layouts.
- Show the global **Ad Skip Pass present** setting directly in Dungeon settings. Enable it only if your account owns the pass. These changes do not automate watching genuine video advertisements.
- Diagnostic ZIPs now include device model/Android, display and actual capture size/strides, DPI/font scale, relevant settings/runtime rules, daily progress and ad confirmation evidence. Metadata excludes activation codes and device serials; screenshots can still show in-game names. The cap remains 50 screenshots per session and 6 stored sessions.

## Validation and known limits

326 automated tests passed, with signed release build and vital lint verified. The local predecessor builds were tested on BlueStacks: automatic Dash reduced green stock from 271 to 264 while the broom remained 2. Network ad tickets were granted/played across separate starts, including a 720x1612 completion back to Home.

This is a regular 5.3.0 release, **not a guarantee of every-device compatibility**. Both Network ads in one fresh uninterrupted daily pass, physical-phone capture/touch differences, Bond bubble collection and remaining device-specific Dungeon/Metal Sea/Auto Summon reports still need current in-app diagnostic recordings. All-Sprite Test remains Beta. If the game exits with security-policy error `00000038` on a phone, disable USB debugging for game runs and share an in-app diagnostic ZIP.

Upgrade the existing app normally to preserve settings and data. The application ID and signing identity are unchanged.
