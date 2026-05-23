#!/usr/bin/env bash
set -euo pipefail

REPOSITORY="$(dirname "$0")"
SCRIPT="${REPOSITORY}/build_appcore.sh"
TARGET="${REPOSITORY}/appcore/target/jniLibs/arm64-v8a/libappcore.so"

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
    echo "appcore is reproducible"
else
    echo "appcore is NOT reproducible"
    exit 1
fi