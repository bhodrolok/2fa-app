#!/usr/bin/env bash
set -euo pipefail

REPOSITORY="$(dirname "$0")"
SCRIPT="${REPOSITORY}/build_standard.sh"
TARGET="${REPOSITORY}/android/app/build/outputs/apk/standard/release/app-standard-release-unsigned.apk"

get_hash() {
    openssl dgst -sha256 "${TARGET}" | awk '{print $NF}'
}

"${SCRIPT}"
HASH1="$(get_hash)"

"${SCRIPT}"
HASH2="$(get_hash)"

echo "$HASH1"
echo "$HASH2"

if [[ "$HASH1" == "$HASH2" ]]; then
    echo "standard apk is reproducible"
else
    echo "standard apk is NOT reproducible"
    exit 1
fi