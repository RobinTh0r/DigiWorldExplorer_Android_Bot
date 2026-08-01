<div align="center">

# ⚡ DigiWorldExplorer Android Bot

**Native, local grid-navigation automation for Android 10+**

[![Version](https://img.shields.io/badge/version-1.0.1-2ea44f?style=for-the-badge)](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/tag/v1.0.1)
[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)
[![Status](https://img.shields.io/badge/status-release_candidate-orange?style=for-the-badge)](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)

## 📱 [DOWNLOAD THE SIGNED APK](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/download/v1.0.1/DigiWorldExplorer-Bot-v1.0.1.apk)

### 📦 [OPEN ALL RELEASES](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases)

`Local processing` · `Deterministic` · `No root` · `No cloud AI` · `Safety first`

</div>

> [!WARNING]
> This is an independent, unofficial fan project. It is not affiliated with or endorsed by any game developer or publisher. Automation may violate a game's terms of service. Use it at your own risk.

> [!IMPORTANT]
> Android may display warnings when installing an APK outside Google Play. Download only from this repository's official **Releases** page. Every official release APK is signed with the same RobinTh0r release certificate.

## ✨ What it does

The app runs directly on an Android device without a PC, BlueStacks, ADB, root, injection, or game-APK modification. It captures the screen locally through `MediaProjection`, detects the visible 5×5 grid, player, collectibles, obstacles and HUD counters, then sends user-authorized gestures through an `AccessibilityService`.

Movement priorities:

1. 🟠🟣🟢 Collect reachable items using safe, short routes
2. ➡️ Explore toward the right with multi-step lookahead
3. 🔺 Prefer detours and preserve claws for real dead ends
4. 💨 Use dash conservatively when escape conditions require it

## 🚀 Quick start

1. **[Download the latest signed APK](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/download/v1.0.1/DigiWorldExplorer-Bot-v1.0.1.apk).**
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
- Unknown claw or dash values are treated conservatively.
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

- The bot can occasionally become stuck even though a manual route is still possible.
- Automatic dash handling is not reliable yet and should currently be considered non-functional.
- If either situation occurs, press **STOP AUTOMATION**, make one or two moves manually, then start automation again.
- The internal claw and dash readings are still used for conservative route planning, but their uncertain `?` labels are intentionally no longer shown in the grid overlay.
## 🧯 Troubleshooting

| Problem | What to do |
| --- | --- |
| Screen-sharing dialog appears again | Android requires fresh consent when a capture session restarts. Confirm it again. |
| Accessibility is disabled after an update | Open the first setup entry and enable DigiWorldExplorer Bot again. |
| No grid appears | Confirm the overlay permission, enable the grid in the app, then bring the game to the foreground. |
| Automation does nothing | Both screen sharing and accessibility control must be active. |
| Grid or player is reported as uncertain | Wait for the scene to settle. Use a small, visually distinct player sprite where possible. |
| Claw or dash counter shows `?` | The number was not read confidently and is handled as nearly empty for safety. |
| Android warns during installation | Verify that the APK came from this repository's Releases page. Sideload warnings are normal for apps outside Google Play. |

## ☕ Support development

If the app is useful to you, you can support continued development through **[PayPal.me/thor666](https://paypal.me/thor666)**. Donations are optional and do not unlock features.

## 📝 Changelog

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
| `accessibility/` | Gesture execution and optional live overlay |
| `app/src/test/` | Offline unit and screenshot-regression tests |

## 🔗 Related project

The [DigiWorldExplorer Windows Bot](https://github.com/RobinTh0r/DigiWorldExplorer_Bot) runs the same general workflow through BlueStacks and ADB.

## ⚖️ License and contributions

No license has been selected yet. Until a license file is added, public source availability does not grant redistribution rights. Focused issues and pull requests are welcome.

<div align="center">

**Explore smart. Stop safely. Collect efficiently.**

[⬇️ Download APK](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/download/v1.0.1/DigiWorldExplorer-Bot-v1.0.1.apk) · [📦 Releases](https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases) · [💻 Windows version](https://github.com/RobinTh0r/DigiWorldExplorer_Bot)

</div>