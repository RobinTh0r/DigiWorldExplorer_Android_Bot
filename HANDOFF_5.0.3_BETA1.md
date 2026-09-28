# DigiWorldExplorer handoff — 5.0.3 Beta 1

Date: 2026-09-28. This is the current transfer-ready working copy, cloned from the public repository's `main` at `003c4fe` (`v5.0.3-beta.1`). The Git repository in **this folder is standalone**. The sibling `.release-v3.2-beta1` is a linked worktree and must not be copied alone. Release: https://github.com/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/tag/v5.0.3-beta.1 . Version 5.0.1 remains the stable release.

## What is here

- Complete tracked Android project, current source, docs, and screenshot regression fixtures.
- `release-apk/DigiWorldExplorer-Bot-v5.0.3-beta.1.apk`: locally copied signed release APK, SHA-256 `F6B0FAD674738B1B410D337BCDA448BC140138EF73AEC67EAD6DE32EC2406D54`. This folder is Git-ignored because the published APK is also on GitHub.
- `docs/RELEASE_NOTES_5.0.3_BETA_1.md` and `CHANGELOG.md` describe the published changes. Older `docs/DEVELOPMENT_STATE.md`, `docs/OPEN_FEATURES.md`, and `docs/COPILOT_SIMPLIFICATION_HANDOFF.md` contain historical observations and may contradict the current code; do not treat their old version/status statements as current.
- No signing secrets, Android SDK, Gradle cache, old diagnostic dumps, or Codex chat database are included.

## Current features and latest fixes

- Digi Co-Pilot includes Bond rotation, Meat Field, Home rewards, and optional 5-minute Digital World Search; Dungeon Co-Pilot is a separate manually started run. VS/Tower Loop and the bot overlay visibility control no longer require Beta access. The old fixed top-left status panel was removed; the compact bubble beside the bot icon remains.
- 5.0.3 Beta 1 improves MuMu dark-city DWS grid recognition; highlighted blue walkable cells remain traversable, and nearby training points are prioritized. The five-minute DWS timeout now belongs only to Digi Co-Pilot, not standalone DWS.
- A Summon reward/card screen guard stops repeated right-side taps during reveals. Auto Summon defaults off on new installs. Existing explicit settings remain preserved.
- Unset Premium options default Bond, Meat Field, rewards and Co-Pilot DWS on; dungeons default selected and three normal attempts. Ad Skip Pass remains opt-in. Do not infer from defaults that the user has granted paid purchases.
- Unit tests and screenshot regressions passed; the release APK was built and its signing certificate matched 5.0.2 Beta 2. **Oppo Reno12 Pro, MuMu Player, and other physical-device flows were not end-to-end verified for this build.** Do not claim them fixed conclusively until ADB testing.

## First checks on the other PC

1. Install Android Studio/JDK and Android SDK platform-tools/build-tools. Start the target emulator or attach the phone with USB debugging; approve its RSA prompt.
2. From this folder run `git status -sb` and `git log -1 --oneline`. Expected base: `003c4fe`; the two local handoff files may be a later local-only commit. Do not reset away user changes.
3. Run `adb devices -l`. At handoff there were **no connected devices** on the source PC; no current ADB run was performed.
4. To install the published APK: `adb install -r release-apk/DigiWorldExplorer-Bot-v5.0.3-beta.1.apk`. If multiple devices are attached, select one with `adb -s SERIAL`.
5. Test in-game with automation supervised. Record model, Android version, game language, resolution, screenshots, current screen/overlay status, and logcat around any failure. In particular test MuMu DWS recognition/training-point detours and uninterrupted standalone DWS beyond five minutes; Oppo Summon card reveal/right-side taps; then Digi Co-Pilot and Dungeon screens on tall phones.
6. For a new signed build, transfer `digiworldexplorer-release.jks` and `keystore.properties` separately from the old PC into this folder, **privately**. They are intentionally Git-ignored; never commit, upload, or paste their contents. The example properties file shows expected keys. The release signing certificate SHA-256 is `859229d0e9163ad0d1ca11aa8ec85c55321a0a50aebca489b3ff775d1e3528f8`; verify future APKs match it.

## Build/test notes

The old machine's build ran `:app:testDebugUnitTest :app:assembleRelease` with Android Studio's JBR, Gradle 9.4.1 and Android build-tools 36.0.0/aapt2. This repository has no Gradle wrapper, so install Gradle 9.4.1 (or use your own verified copy), SDK 36.0.0 and Android Studio JBR on the new PC. `local.properties` and caches are machine-specific and intentionally omitted. The app ID is `de.robinthor.digiworldexplorer`, version code 75, version name `5.0.3-beta.1`.

## Next development priority

Reproduce and finish ADB checks before another release. If DWS still misses training points, capture consecutive frames and inspect `GridDetector`, `CellClassifier`, `MovementPlanner`, and `AutoMoveController`. If Summon still taps repeatedly, inspect `SummonRewardScreenDetector`, `SummonRewardFrameGuard`, `RewardPurchaseFrameAnalyzer`, and capture/automation ordering. For Co-Pilot timeouts inspect `DigiCopilotRequest` and `DwsExcursionAnalyzer`. Keep all purchase actions conservative; an unreadable screen/counter must not be treated as permission to spend.

GitHub has the source and release APK; the local Codex conversation is separate from the Git repository. This handoff is the durable context for a new Codex chat on another computer.

If the original PC remains available and both PCs support Codex Remote, the desktop app can pair the hosts under Settings > Connections and hand off the existing chat to a matching saved project on the destination host. This transfers chat and Git state through Codex; it does **not** transfer ignored signing files or local SDK tools. If Remote is unavailable, open this folder as a local project on the new PC and start a new chat with `AGENTS.md` and this handoff.
