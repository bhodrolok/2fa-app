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
WRAPPER_JAR="${ANDROID}/gradle/wrapper/gradle-wrapper.jar"
REQUIRED_JAVA_VERSION="$(sed -n 's/java = "\(.*\)"/\1/p' "${ANDROID}/gradle/libs.versions.toml")"
JAVA_VERSION="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\)\..*/\1/p')"

if [[ "$JAVA_VERSION" != "$REQUIRED_JAVA_VERSION" ]]; then
    echo "incorrect java version ${JAVA_VERSION} needs to be ${REQUIRED_JAVA_VERSION}"
    exit 1
fi

SOURCE_DATE_EPOCH=$(git --no-pager log -1 --format=%ct) \
java -jar "$WRAPPER_JAR" -q -p "$ANDROID" clean "$GRADLE_TASK" --no-daemon

if [[ "$FORMAT" == "apk" ]]; then
    SDK="$(sed -n 's/^sdk = "\(.*\)"/\1/p' "${ANDROID}/gradle/libs.versions.toml")"
    ZIPALIGN="$ANDROID_HOME/build-tools/${SDK}/zipalign"

    APK_DIR="$ANDROID/app/build/outputs/apk/${1}/release"
    UNSIGNED_APK="$APK_DIR/app-${1}-release-unsigned.apk"
    ALIGNED_APK="$APK_DIR/app-${1}-release-unsigned-aligned.apk"

    "$ZIPALIGN" -P 16 4 "$UNSIGNED_APK" "$ALIGNED_APK"
    "$ZIPALIGN" -c -P 16 4 "$ALIGNED_APK"
fi