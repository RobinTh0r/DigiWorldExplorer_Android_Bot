<div align="center">

# ⚡ DigiWorldExplorer — The DIGIMON UP Bot for Android

**Native, local grid-navigation automation for Android 9+**

[![Version](https://img.shields.io/badge/version-2.2.0-blue?style=for-the-badge)](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)
[![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)
[![Status](https://img.shields.io/badge/status-stable-2ea44f?style=for-the-badge)](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)

## 📱 [DOWNLOAD THE SIGNED APK](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/download/v2.2.0/DigiWorldExplorer-Bot-v2.2.0.apk)

### 📦 [OPEN ALL RELEASES](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)

`Local processing` · `Deterministic` · `No root` · `No cloud AI` · `Safety first`

</div>

> [!WARNING]
> This is an independent, unofficial fan project. It is not affiliated with or endorsed by any game developer or publisher. Automation may violate a game's terms of service. Use it at your own risk.

> [!IMPORTANT]
> Android may display warnings when installing an APK outside Google Play. Download only from this repository's official **Releases** page. Every official release APK is signed with the same RobinTh0r release certificate.

## ✨ What it does

The app runs directly on an Android device without a PC, BlueStacks, ADB, root, injection, or game-APK modification. It captures the screen locally through `MediaProjection`, detects the visible 5×5 grid, player, collectibles, obstacles and HUD counters, then sends user-authorized gestures through an `AccessibilityService`.

### 🧭 Automatic DigiWorld Navigation

The core feature automatically navigates the visible DigiWorld grid. It dynamically detects the 5×5 board, player position, collectibles, obstacles, preview cells and available HUD actions instead of relying on fixed pixel coordinates. The route planner prioritizes reachable energy, explores toward the right, avoids unnecessary obstacle attacks and uses dash conservatively for recovery or suitable blocked paths.

![Automatic DigiWorld navigation with the detected grid, player, items and obstacles](docs/images/automatic-digiworld-navigation.png)

Movement priorities:

1. 🟠🟣🟢 Collect reachable items using safe, short routes
2. ➡️ Explore toward the right with multi-step lookahead
3. 🔺 Prefer detours and preserve claws for real dead ends
4. 💨 Use dash conservatively when escape conditions require it

### ⚔️ VS. Dungeon Automation

The optional **VS. Dungeon** mode automatically recognizes the multilingual challenge dialog, presses **Attempt / Herausfordern**, waits without touching the active battle, closes a detected victory reward screen and starts the next level. Both challenge and reward detection use strict, resolution-independent full-layout fingerprints instead of generic blue-screen matching.

The mode stops after 15 seconds without visible progress. This provides a safe fallback for defeat screens or unexpected dialogs that have not yet been explicitly classified.

![VS Dungeon challenge automatically detected](docs/images/v2.0.0-vs-dungeon.png)

### ⚡ Auto Summon

When enabled in the app, **Auto Summon** recognizes green and purple 30-ticket summon screens and repeatedly presses the yellow summon button. It continues advancing loading/reward screens every 200 ms for up to five seconds. It stops as soon as the 30-ticket cost turns red.

![Auto Summon automatically advancing the reward screen](docs/images/v1.0.4-mega-summon.png)

### 🛡️ Network Defense Ops Loop — BETA

The optional **Network Defense Ops Loop** starts an attempt, waits through all five waves and detects the Diaboromon boss banner together with the final-wave indicator. It then presses **Give up / Aufgeben**, returns to the dungeon dialog and automatically starts the next run. This can count roughly 40 defeated enemies per completed loop.

The mode is **off by default** and must only be enabled inside Network Defense Ops. Its stricter session tracking prevents ordinary battles and the general dungeon overview from triggering the loop. Button positions are detected dynamically for different resolutions and Android display mappings.

![Network Defense Ops detecting Diaboromon before giving up](docs/images/v2.2.0-network-defense-ops.png)
## 🚀 Quick start

1. **[Download the latest signed APK](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/download/v2.2.0/DigiWorldExplorer-Bot-v2.2.0.apk).**
2. Allow installation from your browser or file manager if Android asks.
3. Open DigiWorldExplorer Bot.
4. Complete the two setup permissions. A check mark confirms each one:
   - Accessibility control
   - Display over other apps
5. Start screen sharing.
6. Open the game and press **START**.

You can stop automation at any time from the app or Android notification.

## 🛡️ Safety and privacy

- Every decision uses a newly analyzed frame.
- Grid positions are detected dynamically instead of using fixed coordinates.
- Uncertain grid/player detection or visible error dialogs make the bot wait.
- If a claw or dash HUD digit cannot be read, bounded fallback values keep recovery available; failed actions still trigger the hard safety stop.
- Screenshots and decisions remain on the device.
- Internet access is used only to request the latest release version from GitHub. Screenshots and gameplay data never leave the device.
- The app has no analytics, account system, advertisements, or runtime cloud service.

## 🌐 Languages

The app automatically starts in the device language when it is supported. English is the fallback. German and English can also be selected manually using the flags in the lower-right corner of the app.

## 🧠 Decision flow

```text
MediaProjection frame
        ↓
Detect and stabilize the dynamic 5×5 grid
        ↓
Classify player, items, obstacles, preview and HUD counters
        ↓
Plan the safest action or short verified movement burst
        ↓
Send an accessibility gesture
        ↓
Verify the resulting state before continuing
```

## 🐞 Known issues

- The bot can still choose an imperfect route for unusual player sprites or misclassified objects.
- Dash is used when stuck or when at least two obstacles are detected within three cells ahead, at least two charges remain and no orange energy is visible. One charge is reserved for recovery.
- After five dispatched actions without detected progress, automation, capture and grid stop automatically and Android posts a stuck notification. Move manually before restarting.
- The internal claw and dash readings are still used for conservative route planning, but their uncertain `?` labels are intentionally no longer shown in the grid overlay.

## 📱 Device and emulator compatibility

Compatibility depends on the exact device, Android/OEM build and graphics compositor, not only the Android version. The bot needs Android `MediaProjection` to include the game's complete Unity `SurfaceView`.

| Device / environment | System | Status | Result and reason |
| --- | --- | --- | --- |
| Samsung Galaxy S21 Ultra (`SM-G998B`) | Physical device, 1080×2400 | ✅ Working | Full frames, dynamic grid, dark scenes and automated movement tested. Samsung may require **Allow restricted settings** for Accessibility. |
| BlueStacks 5 (`5.22.245.1004`) | Android 11 emulator | ✅ Working | Capture, overlay, grid recognition and accessibility gestures tested successfully. |
| BlueStacks 5 Pie 64 | Android 9 / Pie 64 emulator | ✅ Working | Screen capture, DigiWorld navigation, Auto Summon and accessibility gestures tested successfully. |
| OnePlus 8 Pro EU (`IN2023`) | OxygenOS 13.1 / Android 13 | ✅ Working | Screen capture, recognition and automation tested successfully after the capture/display-metrics compatibility improvements. |
| LDPlayer | Exact build not recorded | ⚠️ Unconfirmed failure | Automation did not work in the reported test. Without the emulator version, Android image and diagnostic frame, the precise cause is still unknown. |

When reporting another device, include its exact model, Android version, OEM build, bot version, whether the system recorder captures the game, and the app's `dynamic_grid.png`. Remove private notifications or account information before sharing diagnostics.

## 🧯 Troubleshooting

| Problem | What to do |
| --- | --- |
| Screen-sharing dialog appears again | Android requires fresh consent when a capture session restarts. Confirm it again. |
| Accessibility is disabled or blocked | On Samsung, first try enabling it once so the blocked-access warning appears. Then open **Settings → Apps → DigiWorldExplorer Bot → ⋮ → Allow restricted settings**. Return to Accessibility and enable it again. The three-dot option may only appear after the first failed attempt. |
| No grid appears | Confirm the overlay permission, enable the grid in the app, then bring the game to the foreground. |
| Automation does nothing | Both screen sharing and accessibility control must be active. |
| Grid or player is reported as uncertain | Wait for the scene to settle. Use a small, visually distinct player sprite where possible. |
| Dash or claw behavior differs from the visible counter | Some resolutions cannot read the HUD digit reliably. The bot uses bounded recovery fallbacks and stops after repeated failed actions. |
| Android warns during installation | Verify that the APK came from this repository's Releases page. Sideload warnings are normal for apps outside Google Play. |
| HUD is visible but the game area is gray or black | The OEM compositor is excluding the Unity surface from third-party MediaProjection. A working hardware screenshot or built-in recorder does not prove that ordinary apps receive the same image. Check the compatibility table above. |

## ☕ Support development

If the app is useful to you, you can support continued development through **[PayPal.me/thor666](https://paypal.me/thor666)**. Donations are optional and do not unlock features.

## 📝 Changelog

### v2.2.0 — 8 August 2026

- 🛡️ Added the optional **Network Defense Ops Loop (BETA)**
- ▶️ Starts Network Defense Ops attempts automatically and waits through all five waves
- 👾 Detects the Diaboromon boss banner and final-wave indicator before giving up
- 🔁 Returns to the dungeon dialog and repeats the run automatically
- 🎯 Detects the Give up button dynamically and compensates for emulator touch-coordinate offsets
- 🧭 Requires an active bot-started session, preventing ordinary battles from triggering the boss action
- 🚫 Tightened start-dialog detection so the general dungeon overview is no longer mistaken for Network Defense Ops
- ⏳ Keeps the Diaboromon status stable during transitions and retains the existing safety timeout
- 📱 Improved MediaProjection and display-metrics compatibility; successfully tested on OnePlus/OxygenOS, Samsung, BlueStacks Android 11 and BlueStacks Pie 64, with the fallback potentially helping additional devices and emulators

### v2.0.0 — 7 August 2026

- ⚔️ Added optional **VS. Dungeon automation** for the new daily multi-level dungeon battles
- ▶️ Detects and presses multilingual **Attempt / Herausfordern** dialogs automatically
- 🏆 Recognizes victory reward screens, closes them and advances to the next dungeon level
- 🌐 Uses resolution-independent full-layout fingerprints instead of translated text or generic blue colors
- 🛑 Stops VS. Dungeon after 15 seconds without visible progress for safe defeat/unknown-screen handling
- 🎛️ Added separate Auto Summon and VS. Dungeon switches with illustrated bilingual help pages
- 📐 Redesigned the app into a compact single-screen control layout with side-by-side setup, status, update and stop controls
- 🧪 Added German/English screenshot regression tests and negative coverage for ordinary blue game screens
- ✅ Passed all 52 automated tests and verified the signed build on BlueStacks 5
### v1.0.4 — 3 August 2026

- ⚡ Added optional **Mega Summon / Auto Summon** for green and purple ticket screens
- ⏱️ Continues through loading and reward screens every 200 ms for up to five seconds
- 🛑 Re-checks affordability and stops when the 30-ticket cost turns red
- 🟡 Recognizes yellow-black claw drops even when the bottom wall partly covers them
- 💨 Restored proactive dash with unreadable HUD digits and detects A–B–A–B loops immediately
- 🧭 Improved stuck recovery, route progress tracking and the five-action safety stop
- 🫥 Hides the green grid immediately outside DigiWorld while keeping a clear status message
- 🔋 Stops capture and automation after one minute without DigiWorld or a summon screen
- 🐾 Shows a Botamon-sprite hint when the player cannot be identified reliably
- 🌐 Added localized English/German labels, compatibility guidance and an Auto Summon toggle
- 🧪 Passed all 49 unit and screenshot-regression tests; verified on BlueStacks 5

### v1.0.3 — 3 August 2026


- 🐾 Improved recognition of small red claw drops so they are less likely to be marked as obstacles
- 💨 Enabled proactive dash when two obstacles are visible within the next three cells, at least two charges remain and no orange energy is visible; one dash stays reserved for recovery
- 🚶 Changed stuck tracking from analysis frames to dispatched actions and real board/rightward progress
- 🛑 Added a hard safety stop after five actions without progress, including capture/grid shutdown and a localized Android notification
- 🧪 Added regression coverage for the new dash threshold and visible-energy protection
- 🔍 Audited v1.0.1 versus v1.0.2: capture, detection, accessibility configuration and movement logic were unchanged; v1.0.2 only added permission-help UI/text and version metadata
### v1.0.2 — 1 August 2026


- ℹ️ Added an in-app **Blocked?** help button next to the accessibility setup
- 📱 Added the Samsung-specific restricted-settings instructions in English and German
- 📖 Documented that Samsung may show the three-dot permission option only after one failed activation attempt
### v1.0.1 — 1 August 2026

- 🌐 Localized all grid-overlay status messages in German and English
- 🧹 Removed the visible `Claws ?` / `Dash ?` HUD labels from the overlay
- 🐞 Documented the current stuck-state and dash limitations with manual recovery steps
### v1.0.0 — 1 August 2026

- 🔄 Added automatic and manual update checks against official GitHub Releases
- ⬇️ Added a safe link to open the latest official release when an update is available
- ☕ Restored the PayPal support link in the app and README
- 📐 Added more breathing room above the compact control UI
- 🏷️ Promoted the tested public release candidate to the first stable release

### v0.3.0-rc1 — 1 August 2026

- 📱 Redesigned the setup screen into a more compact, scrollable layout
- ✅ Added live status checks and check marks for accessibility and overlay permissions
- 🌐 Added complete English and German UI resources
- 🗣️ Added automatic device-language selection and manual DE/EN controls
- 🔔 Localized foreground-service notifications and stop actions
- 🎨 Replaced the franchise-like icon with an original digital explorer mascot
- 📖 Reworked the repository documentation for a public international release
- 🔏 Built and verified the signed release APK with the RobinTh0r RSA certificate

### v0.2.5 — 31 July 2026

- 🟠 Prioritized orange energy over other collectibles when the detour is at most three cells
- 🚪 Added pyramid dead-end detection and avoidance when the right side can no longer be reached

### v0.2.4 — 31 July 2026

- 🎨 Added the previous custom app icon and removed its unwanted white background

### v0.2.3 — 31 July 2026

- ⏱️ Set the tap interval to 800 ms for cleaner frame analysis between gestures
- 🚫 Movement bursts now stop immediately when an in-game error dialog appears

### v0.2.2 — 31 July 2026

- 💡 Added guidance about using a small, clearly distinguishable player sprite
- 📦 Renamed the repository to `DigiWorldExplorer_Android_Bot`
- 🧹 Removed internal planning documents from the public project

### v0.2.1 — 31 July 2026

- 🐞 Added detection for the red claw collectible
- 💨 Added dash recovery when the player oscillates without making progress
- 🛡️ Prevented failed dash attempts from blocking automation indefinitely

### v0.2.0 — 31 July 2026

- 🔢 Added claw and dash HUD counter detection
- 🔺 Added claw reserve logic and obstacle detours
- 🏃 Added safe multi-step movement bursts
- 🐞 Prevented dialog text from being selected as the player
- ⚡ Improved frame-analysis performance by approximately 1.6×
- 🔏 Added version display, release signing, donation link and a custom icon

### v0.1.0 — 29 July 2026

- 🧭 Added dynamic detection of the visible 5×5 grid
- 🟠 Added prioritized item collection
- 🛑 Added safety stops for uncertain grid or player detection

## 🧪 Build and test

Open the project with Android Studio and JDK 17.

```powershell
./gradlew testDebugUnitTest assembleDebug
./gradlew assembleRelease
```

Release signing reads local values from `keystore.properties`. The password file and `.jks` keystore are intentionally ignored by Git and must never be committed.

## 📂 Project structure

| Path | Purpose |
| --- | --- |
| `app/src/main/java/.../MainActivity.kt` | Compact setup, permission state and control UI |
| `capture/` | MediaProjection capture and frame analysis |
| `detection/` | Grid, player, item, obstacle and HUD detection |
| `strategy/` | Movement planning and automation state |
| `dungeon/` | Strict VS. Dungeon challenge/reward recognition and safe auto-advance |
| `accessibility/` | Gesture execution and optional live overlay |
| `app/src/test/` | Offline unit and screenshot-regression tests |

## 🔗 Related project

The [DigiWorldExplorer Windows Bot](https://github.com/RobinTh0r/DigiWorldExplorer_Bot) runs the same general workflow through BlueStacks and ADB.

## ⚖️ License and contributions

No license has been selected yet. Until a license file is added, public source availability does not grant redistribution rights. Focused issues and pull requests are welcome.

<div align="center">

**Explore smart. Stop safely. Collect efficiently.**

[⬇️ Download APK](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/download/v2.2.0/DigiWorldExplorer-Bot-v2.2.0.apk) · [📦 Releases](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases) · [💻 Windows version](https://github.com/RobinTh0r/DigiWorldExplorer_Bot)

</div>