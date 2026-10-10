# DigiWorldExplorer 5.5.1 Beta 1

Targeted Dungeon-result recovery update based on a real Samsung S25 diagnosis. Update in place; existing app identity, signing certificate, settings and activation codes retained.

## Changes

- Late Dungeon Failed guides remain owned by the active rotation even after a returned-menu transition; repeated guide frames do not count as extra defeats.
- Returning to an Attempt/Challenge panel is no longer counted as a victory without positive reward/result evidence. Returned panels must be stable before resolving the transition.
- Consecutive actual defeats stop further retries at the configured normal-attempt count (default three); a verified win resets the streak. Successful DemiDevimon/Bakemon progression retains the existing15-attempt ceiling.
- Completed panels close through the right backdrop instead of bottom navigation; foreground failure guides cannot authorize background panel actions.
- Global Stage Failed recognition distinguishes gray Growth Guide headers from white/pink announcements introduced by the game update.
- Ticket digits with holes (including8/9) no longer become zero solely because of the hole. A foreground dialog body is required for repeatable Dungeon panels; cyan battle effects/hand cards cannot authorize another Start.
- Accepted combat transitions cannot be retried as an unaccepted Start. Unconfirmed result returns are counted separately and park at the configured attempt count without marking the card complete.
- Added result and loss-limit diagnosis events plus S25-image/state regression coverage.
- Bond & Friendship (Classic and Co-Pilot): tap gently inside the measured bubble's upper-right interior instead of its center. Always use the current frame's bubble and the same measured viewport for input; no blind figure-center fallback or tiny-target jitter. Diagnostics include bounds, viewport and tap zone.

## Verification

725 automated tests passed (zero failures/errors/skips); signed release build and vital lint passed. S25 package reports1080×2340, DPI420, fontScale1.15. BlueStacks was natively configured to those values and restarted; Android input/display dimensions verified. Live testing reproduced false-empty9/8 ticket counters and mid-battle card taps. The corrected dialog guard and transition ownership eliminated those taps during the repeated DemiDevimon388 test; returned panels were logged as unconfirmed rather than WIN.

Bond follow-up: six existing OnePlus/native bubble images, Popup/vanished-bubble negatives and four synthetic scales (1080×2340,1080×2400,720×1612,1440×3200), each with/without margins, verify the target is upper-right INSIDE the observed bubble and maps through its viewport. These are image/unit checks, not newly performed native emulator or phone runs. The S25 ZIP has Feed/Bond/Copilot disabled and cannot prove this Bond symptom's cause.

This is a targeted recovery beta, not certification of a successful fresh Dungeon clear/reward or advertisement. The game kept returning from difficulty388 without a captured result. Final build live test: exactly three starts/unconfirmed returns, then PARK, no fourth Start, no false WIN and no mid-battle card taps. The existing left-hand "Clear Previous Difficulty" action remains unimplemented; this beta does not change to that action or promise a weaker party can win.

Matching resolution/DPI/font scale does not reproduce Samsung Android16, One UI, game account/party strength or every inset/navigation behavior. BlueStacks remains Android9; no physical S25 gameplay test was performed. Previous5.5.0 limitations, including fresh advertising validation, remain.

On physical phones, USB debugging must be OFF during gameplay (security error00000038). Enable Diagnose before the failure and export the stopped session afterward. "Dungeon Failed" can also mean the game battle was genuinely lost; these fixes do not make a weaker party win.

## Deutsch

Gezielte Dungeon-Korrektur: Späte Fehlerdialoge werden weiter geschlossen, die bloße Rückkehr zum Startfenster zählt nicht mehr als Sieg und wiederholte Dialogbilder werden nicht mehrfach als Niederlage gezählt. Nach der eingestellten Anzahl aufeinanderfolgender Niederlagen kehrt die Rotation zurück, statt blind weiterzuprobieren. Neue pinke Spielankündigungen sollen nicht mehr als „Stage Failed“ erkannt werden. S25-Rückmeldung und native BlueStacks-Nachstellung sind getrennt dokumentiert; keine Garantie für jedes Handy.

Bond & Friendship und Co-Pilot tippen dezent oben rechts innerhalb der frisch erkannten Blase, nicht blind auf die Figurmitte. Erkennung und Klick verwenden denselben beobachteten Spielbereich. Diese Bond-Änderung wurde mit Bildern und automatisierten Tests geprüft, nicht in einem neuen Handy-Lauf. Insgesamt725 Tests bestanden; erfolgreiche neue Dungeon-Belohnungen/Werbung und das Abschließen der vorherigen Schwierigkeit bleiben offen.
