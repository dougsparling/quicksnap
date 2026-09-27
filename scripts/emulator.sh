#!/bin/bash
# Opt-in Android emulator for Claude Code on the web containers.
#
# These containers have no KVM, so the emulator runs in software (TCG) mode:
# boot takes 5-20 minutes and the guest needs several workarounds, applied below.
# Prefer CI (GitHub runners have KVM) for anything routine.
#
# Usage: scripts/emulator.sh <setup|start|stop|install|screenshot FILE>
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
AVD=qs37
IMAGE="system-images;android-37.0;google_apis;x86_64"
PKG=dev.cyberdeck.qs
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD_DIR="${ANDROID_AVD_HOME:-$HOME/.android/avd}/$AVD.avd"
LOG="$HOME/emu.log"

wait_boot() {
  until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do
    pgrep -x qemu-system-x86 >/dev/null || { echo "emulator died, see $LOG" >&2; exit 1; }
    sleep 15
  done
}

cmd_setup() {
  # ~6 GB of downloads.
  local sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
  yes | "$sdkmanager" --licenses >/dev/null 2>&1 || true
  "$sdkmanager" "emulator" "$IMAGE" >/dev/null
  if [ ! -d "$AVD_DIR" ]; then
    echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n "$AVD" -k "$IMAGE" >/dev/null
  fi
  # Emulated cameras for CameraX, more RAM for software mode, and a 4 GB /data:
  # the default 800 MB is already full of preloaded app updates, so the debug APK won't fit.
  local cfg="$AVD_DIR/config.ini" kv
  for kv in hw.camera.back=emulated hw.camera.front=emulated hw.ramSize=4096 \
            hw.cpu.ncore=4 disk.dataPartition.size=4G; do
    sed -i "/^${kv%%=*}=/d" "$cfg"
    echo "$kv" >> "$cfg"
  done
  echo "AVD $AVD ready"
}

cmd_start() {
  if pgrep -x qemu-system-x86 >/dev/null; then echo "already running"; return; fi
  # Free RAM for the emulator; idle Gradle and Kotlin daemons use several GB.
  (cd "$(dirname "$0")/.." && ./gradlew --stop >/dev/null 2>&1) || true
  # -writable-system keeps the build.prop edit below across boots.
  (cd ~ && nohup "$EMULATOR" -avd "$AVD" -accel off -no-window -no-audio \
    -gpu swiftshader_indirect -no-snapshot -no-boot-anim -writable-system > "$LOG" 2>&1 &)
  echo "booting (software mode, this is slow)..."
  $ADB wait-for-device
  wait_boot

  # The system watchdog kills system_server for being slow under TCG. Raise its
  # timeout; -prop doesn't apply to ro.* here, so write build.prop and reboot once.
  if [ "$($ADB shell getprop ro.hw_timeout_multiplier | tr -d '\r')" != 30 ]; then
    $ADB root >/dev/null; sleep 5; $ADB wait-for-device
    $ADB remount >/dev/null
    $ADB shell 'grep -q hw_timeout_multiplier /system/build.prop || echo ro.hw_timeout_multiplier=30 >> /system/build.prop'
    $ADB reboot
    sleep 10; $ADB wait-for-device
    wait_boot
  fi

  # Gesture navigation crashes SurfaceFlinger with swiftshader; use 3-button nav.
  until $ADB shell cmd overlay list 2>/dev/null | grep -q navbar.threebutton; do sleep 5; done
  $ADB shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
  echo "booted"
}

cmd_stop() {
  $ADB emu kill >/dev/null 2>&1 || true
  while pgrep -x qemu-system-x86 >/dev/null; do sleep 2; done
  echo "stopped"
}

cmd_install() {
  # The app expects its permissions granted before launch (see README).
  local apk
  apk="$(dirname "$0")/../app/build/outputs/apk/debug/app-debug.apk"
  $ADB install -r "$apk"
  $ADB shell pm grant $PKG android.permission.CAMERA
  $ADB shell pm grant $PKG android.permission.POST_NOTIFICATIONS
  # Under TCG the install can finish processing after launch and clear the
  # foreground notification, so force a clean start.
  $ADB shell am force-stop $PKG
  $ADB shell am start -W -n $PKG/.MainActivity
}

cmd_screenshot() {
  # `adb shell screencap` hits the same swiftshader readback bug; capture host-side.
  $ADB emu screenrecord screenshot "$(realpath -m "$1")"
}

case "${1:-}" in
  setup|start|stop|install) "cmd_$1" ;;
  screenshot) cmd_screenshot "${2:?usage: $0 screenshot FILE}" ;;
  *) echo "usage: $0 <setup|start|stop|install|screenshot FILE>" >&2; exit 2 ;;
esac
