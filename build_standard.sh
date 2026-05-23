#!/usr/bin/env bash
set -euo pipefail

ANDROID="$(dirname "$0")/android"
REQUIRED_GRADLE_VERSION=$(grep "distributionUrl" "$ANDROID/gradle/wrapper/gradle-wrapper.properties" | grep -o -E "gradle-[0-9]+\.[0-9]+\.[0-9]+" | cut -d'-' -f2)

GRADLE_INFO=$(gradle --version)
GRADLE_VERSION=$(echo "$GRADLE_INFO" | grep "Gradle " | awk '{print $2}')

if [[ "$GRADLE_VERSION" != "$REQUIRED_GRADLE_VERSION" ]]; then
    echo "incorrect gradle version $GRADLE_VERSION needs to be $REQUIRED_GRADLE_VERSION"
    exit 1
fi

REQUIRED_JAVA_VERSION="25.0.3"
REQUIRED_JAVA_VENDOR="Eclipse Adoptium"

JVM_LAUNCHER=$(echo "$GRADLE_INFO" | grep "Launcher JVM:")

JVM_VERSION=$(echo "$JVM_LAUNCHER" | awk '{print $3}')
JVM_VENDOR=$(echo "$JVM_LAUNCHER" | cut -d'(' -f2 | cut -d' ' -f1,2)

if [[ "$JVM_VERSION" != "$REQUIRED_JAVA_VERSION" ]]; then
    echo "incorrect jvm version $JVM_VERSION needs to be $REQUIRED_JAVA_VERSION"
    exit 1
fi

if [[ "$JVM_VENDOR" != "$REQUIRED_JAVA_VENDOR" ]]; then
    echo "incorrect jvm vendor $JVM_VENDOR needs to be $REQUIRED_JAVA_VENDOR"
    exit 1
fi

rm -rf "$ANDROID/.gradle"
rm -rf "$ANDROID/build"
rm -rf "$ANDROID/app/build"

SOURCE_DATE_EPOCH=$(git --no-pager log -1 --format=%ct) \
gradle -p "$ANDROID" clean assembleStandardRelease --no-daemon