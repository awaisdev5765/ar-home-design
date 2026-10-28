#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_USER_HOME_DIR="$PROJECT_DIR/.gradle-home"
mkdir -p "$GRADLE_USER_HOME_DIR"

JAVA17="/Library/Java/JavaVirtualMachines/openjdk17.0.1/Contents/Home"
JAVA_HOMEBREW="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
if [ -d "$JAVA17" ]; then
  JAVA_HOME_USE="$JAVA17"
elif [ -d "$JAVA_HOMEBREW" ]; then
  JAVA_HOME_USE="$JAVA_HOMEBREW"
else
  echo "Java not found. Install JDK 17 and retry."
  exit 1
fi

if [ -x "/opt/homebrew/opt/gradle@8/bin/gradle" ]; then
  GRADLE_BIN="/opt/homebrew/opt/gradle@8/bin/gradle"
elif [ -x "/opt/homebrew/bin/gradle" ]; then
  GRADLE_BIN="/opt/homebrew/bin/gradle"
else
  echo "Gradle not found. Install Gradle 8 (brew install gradle@8)."
  exit 1
fi

env -i \
  HOME="${HOME:-/Users/airtechsolutions}" \
  USER="${USER:-airtechsolutions}" \
  PATH="/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin" \
  JAVA_HOME="$JAVA_HOME_USE" \
  GRADLE_USER_HOME="$GRADLE_USER_HOME_DIR" \
  "$GRADLE_BIN" \
  --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8" \
  assembleRelease

echo ""
echo "Release APK generated at:"
echo "  $PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
