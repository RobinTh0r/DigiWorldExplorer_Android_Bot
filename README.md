# DigiWorldExplorer Android Bot

![Version](https://img.shields.io/badge/version-0.3.0--rc1-blue) ![Status](https://img.shields.io/badge/status-release%20candidate-orange) ![Platform](https://img.shields.io/badge/platform-Android%2010%2B-green)

A native, local Android grid-navigation assistant. It analyzes the visible game board with Android MediaProjection and performs user-authorized gestures through an Accessibility Service. It does not modify, inject into, or redistribute the game APK and requires no root, PC, ADB, or cloud service at runtime.

> [!WARNING]
> This is an independent, unofficial fan project and is not affiliated with or endorsed by any game developer or publisher. Automation may violate a game's terms of service. Use it at your own risk.

## Features

- Dynamic 5×5 grid, player, item, obstacle, preview, and HUD detection
- Local deterministic path planning with energy priority and dead-end avoidance
- Conservative claw and dash handling
- Optional live grid overlay
- Immediate stop from the app or foreground notification
- English and German UI with automatic device-language selection and manual switching
- No analytics, accounts, advertising, cloud AI, root, injection, or fixed tap coordinates

## Requirements

- Android 10 (API 29) or newer
- Accessibility control enabled by the user
- Display-over-other-apps permission for the optional grid
- Screen-sharing consent after each capture session starts

## Install and run

1. Download the signed APK from GitHub Releases.
2. Install it. For APKs distributed outside Google Play, Android may ask you to allow installation from that source.
3. Open the app and complete the two setup entries. A check mark confirms each permission.
4. Start screen sharing.
5. Open the game and press **START**.

Stop automation at any time using **STOP AUTOMATION**, **Stop everything**, or the Android notification.

## Privacy and safety

All screenshots and decisions remain on the device. The app does not request internet access. If the grid or player cannot be detected reliably, the controller waits instead of guessing. Accessibility gestures only run after the user explicitly starts automation.

## Build

Open the project with Android Studio and use JDK 17. Command-line examples:

```powershell
./gradlew testDebugUnitTest assembleDebug
./gradlew assembleRelease
```

Release signing reads local values from `keystore.properties`; secrets and the keystore must never be committed. See `keystore.properties.example`.

## Project structure

| Path | Purpose |
| --- | --- |
| `app/src/main/java/.../MainActivity.kt` | Compact setup and controls UI |
| `capture/` | MediaProjection capture and frame analysis |
| `detection/` | Grid, player, item, obstacle, and HUD detection |
| `strategy/` | Movement planning and automation state |
| `accessibility/` | Gesture execution and optional overlay |
| `app/src/test/` | Offline unit and screenshot regression tests |

## Release checklist

- Update `versionCode` and `versionName`
- Run unit tests and build both APK variants
- Verify the release certificate with `apksigner verify --verbose --print-certs`
- Test setup, language selection, start, stop, and overlay on a real device
- Publish the signed APK with matching tag and release notes

## License and contributions

No license has been selected yet. Until a license file is added, copyright remains with the author and public source availability does not grant redistribution rights. Issues and focused pull requests are welcome.

Related Windows/BlueStacks project: [DigiWorldExplorer_Bot](https://github.com/RobinTh0r/DigiWorldExplorer_Bot)