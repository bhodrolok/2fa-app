#!/usr/bin/env bash
set -euo pipefail

FLAVOR="$(tr '[:lower:]' '[:upper:]' <<< "${1:0:1}")${1:1}"
FORMAT="$2"

if [[ "$FORMAT" == "apk" ]]; then
    GRADLE_TASK="assemble${FLAVOR}Release"
else
    GRADLE_TASK="bundle${FLAVOR}Release"
fi

ANDROID="$(dirname "$0")/android"
REQUIRED_GRADLE_VERSION=$(grep "distributionUrl" "$ANDROID/gradle/wrapper/gradle-wrapper.properties" | grep -o -E "gradle-[0-9]+\.[0-9]+\.[0-9]+" | cut -d'-' -f2)

GRADLE_INFO=$(gradle --version)
GRADLE_VERSION=$(echo "$GRADLE_INFO" | grep "Gradle " | awk '{print $2}')

if [[ "$GRADLE_VERSION" != "$REQUIRED_GRADLE_VERSION" ]]; then
    echo "incorrect gradle version $GRADLE_VERSION needs to be $REQUIRED_GRADLE_VERSION"
    exit 1
fi

REQUIRED_JVM_VERSION="25.0.3"
REQUIRED_JVM_VENDOR="Eclipse Adoptium"

JVM_LAUNCHER=$(echo "$GRADLE_INFO" | grep "Launcher JVM:")

JVM_VERSION=$(echo "$JVM_LAUNCHER" | awk '{print $3}')
JVM_VENDOR=$(echo "$JVM_LAUNCHER" | cut -d'(' -f2 | cut -d' ' -f1,2)

if [[ "$JVM_VERSION" != "$REQUIRED_JVM_VERSION" ]]; then
    echo "incorrect jvm version $JVM_VERSION needs to be $REQUIRED_JVM_VERSION"
    exit 1
fi

if [[ "$JVM_VENDOR" != "$REQUIRED_JVM_VENDOR" ]]; then
    echo "incorrect jvm vendor $JVM_VENDOR needs to be $REQUIRED_JVM_VENDOR"
    exit 1
fi

rm -rf "$ANDROID/.gradle"
rm -rf "$ANDROID/build"
rm -rf "$ANDROID/app/build"

SOURCE_DATE_EPOCH=$(git --no-pager log -1 --format=%ct) \
gradle -p "$ANDROID" clean "$GRADLE_TASK" --no-daemon

if [[ "$FORMAT" == "apk" ]]; then
    BUILD_TOOLS_VERSION=$(grep "buildToolsVersion =" "$ANDROID/app/build.gradle.kts" | grep -o -E '"[0-9]+(\.[0-9]+)+"' | tr -d '"')
    ZIPALIGN="$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION/zipalign"

    APK_DIR="$ANDROID/app/build/outputs/apk/${1}/release"
    UNSIGNED_APK="$APK_DIR/app-${1}-release-unsigned.apk"
    ALIGNED_APK="$APK_DIR/app-${1}-release-unsigned-aligned.apk"

    $ZIPALIGN -v -P 16 4 "$UNSIGNED_APK" "$ALIGNED_APK"
fi