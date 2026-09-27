#!/bin/bash
# Installs the Android SDK pieces needed to build and lint quicksnap in
# Claude Code on the web sessions. The emulator is opt-in: see scripts/emulator.sh.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

export ANDROID_HOME="$HOME/android-sdk"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
PACKAGES=("platform-tools" "platforms;android-37.0" "build-tools;37.0.0")

if [ ! -x "$SDKMANAGER" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  cd "$ANDROID_HOME"
  zip=$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml \
    | grep -o 'commandlinetools-linux-[0-9]*_latest.zip' | sort -u | tail -1)
  curl -fsSL -o clt.zip "https://dl.google.com/android/repository/$zip"
  rm -rf cmdline-tools/latest
  unzip -q clt.zip -d cmdline-tools
  mv cmdline-tools/cmdline-tools cmdline-tools/latest
  rm clt.zip
fi

if [ ! -d "$ANDROID_HOME/platforms/android-37.0" ] || [ ! -d "$ANDROID_HOME/build-tools/37.0.0" ] \
    || [ ! -x "$ANDROID_HOME/platform-tools/adb" ]; then
  yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
  "$SDKMANAGER" "${PACKAGES[@]}" >/dev/null
fi

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export ANDROID_HOME=\"$ANDROID_HOME\"" >> "$CLAUDE_ENV_FILE"
  echo "export PATH=\"\$PATH:$ANDROID_HOME/platform-tools\"" >> "$CLAUDE_ENV_FILE"
fi
