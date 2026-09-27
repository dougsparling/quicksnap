# quicksnap

Android test-bed app (Kotlin, Compose, CameraX) that runs the camera from a foreground service.
minSdk 35, compile/target SDK 37. See README.md for background.

## Build and check

```
./gradlew assembleDebug lintDebug testDebugUnitTest
```

This is exactly what CI runs (`.github/workflows/ci.yml`) on pushes to `main` and on PRs.
Keep lint clean: there is no lint baseline, so any lint error fails CI.

## Claude Code on the web

- `.claude/hooks/session-start.sh` installs the Android SDK (platform-tools, platform 37,
  build-tools 37) into `~/android-sdk` and sets `ANDROID_HOME` at session start.
- The emulator is **not** installed by default. The containers have no KVM, so it runs in
  slow software mode (5–20 min boots). Only use it when a change needs on-device
  verification; otherwise rely on the build, lint, and CI.
- Opt in with `scripts/emulator.sh`:
  ```
  scripts/emulator.sh setup        # ~6 GB download: emulator, API 37 image, AVD "qs37"
  scripts/emulator.sh start        # boots and applies the workarounds below
  ./gradlew assembleDebug && scripts/emulator.sh install   # install, grant perms, launch
  scripts/emulator.sh screenshot out.png
  scripts/emulator.sh stop
  ```
  Run `start` in the background; it blocks until boot completes.
- Emulator workarounds the script applies, and why:
  - `ro.hw_timeout_multiplier=30` in `/system/build.prop` (needs `-writable-system`):
    the watchdog otherwise kills `system_server` for being slow.
  - 3-button navigation: gesture nav crashes SurfaceFlinger under swiftshader.
  - 4 GB `/data`: the default 800 MB is full before the ~80 MB debug APK is installed.
  - Screenshots via the emulator console (`adb emu screenrecord screenshot`):
    `adb shell screencap` hits the same graphics bug.
  - Force-stop and relaunch after install: a slow install can clear the foreground
    service notification if the app was already running.
  - `./gradlew --stop` before boot to free RAM for the emulator.
- Expect it to be slow even once booted: ~6 min for `adb install`, ~10 min until the
  foreground notification is posted, and SystemUI can take a minute to draw the shade
  after `cmd statusbar expand-notifications`. Check state with `dumpsys` before
  trusting a screenshot, and retake it if the shade looks half-drawn.
