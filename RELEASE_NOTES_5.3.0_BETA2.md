# DigiWorldExplorer 5.3.0 Beta 2

This is a test release. It keeps the existing app ID and signing certificate, so it installs over earlier releases without clearing settings or game data.

## Changes

- DWS V3 Classic and V4 Dash are free. Only V5 All Sprites + Dash needs a Beta code. V4 is the default for new users; an existing V3/V4 choice is kept across app and capture restarts.
- Bond Rotation now finds the Partner panel, roster Plus and confirmation buttons from visible screen geometry. It also has bounded recovery for an accidentally opened Digimon detail popup.
- Copilot Home rewards now find the chest, dialog and claim controls visually instead of relying on German text at fixed 720-pixel positions. Unconfirmed taps retry only while the same screen remains visible, and a failed reward sequence stops with diagnostic evidence.
- Active Bond, reward and Network Defense sessions keep ownership of transitional frames. A World Search grid also suppresses unrelated title-screen taps.
- Dungeon Rotation hides the large right-hand status card. Active Copilot shows a small animated indicator beneath the floating icon, designed to let touches pass through.

## Still under investigation

This Beta has **not** been confirmed on every physical phone. Reports of early Dungeon returns, missed free/ad attempts in Network Ops or Metal Sea, Bond stalls on Samsung S24/S25 and BlueStacks, DWS dash/loop problems, and Auto Summon problems are **not** being presented as fixed. Some reports still need a complete in-app diagnostic session from before the failure until it occurs. A very small ZIP containing no game frames cannot establish the cause.

If you encounter one of these, enable Experimental diagnostics, run only the failing feature until it fails, then share the full diagnostic ZIP and your phone model/resolution. For phones where the game closes with security error `00000038` while USB debugging is on, disconnect ADB and use in-app diagnostics instead.
