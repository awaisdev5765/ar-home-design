#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_DIR="/Users/airtechsolutions/Library/Android/sdk"
ADB="$SDK_DIR/platform-tools/adb"

if [ ! -x "$ADB" ]; then
  echo "adb not found at $ADB"
  echo "Install Android SDK platform-tools first."
  exit 1
fi

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
  echo "Debug APK not found. Building it first..."
  "$PROJECT_DIR/build-debug-apk.sh"
fi

if ! "$ADB" devices | awk 'NR>1 && $2=="device" {found=1} END {exit found?0:1}'; then
  echo "No running emulator/device found."
  echo "Start an emulator in Android Studio, then run this script again."
  exit 1
fi

"$ADB" install -r "$APK_PATH"
"$ADB" shell am start -n com.arhomedesign/.MainActivity

echo "App installed and launched on connected emulator/device."
