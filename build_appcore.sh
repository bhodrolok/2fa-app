#!/usr/bin/env bash
set -euo pipefail

OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"

if [[ "$OS" == "darwin" && "$ARCH" == "arm64" ]]; then
    ARCH="x86_64"
fi

PROVISIONING_NDK="${PROVISIONING_NDK:-30.0.14904198}"
ANDROID_NDK="${ANDROID_HOME}/ndk/${PROVISIONING_NDK}"
NDK_BIN="${ANDROID_NDK}/toolchains/llvm/prebuilt/${OS}-${ARCH}/bin"

APPCORE="$(dirname "$0")/appcore"

if [[ -d "${APPCORE}/target" ]]; then
    rm -rf "${APPCORE}/target"
fi

if [[ ! -f "${APPCORE}/Cargo.lock" ]]; then
    cargo generate-lockfile --manifest-path "${APPCORE}/Cargo.toml"
fi

CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="${NDK_BIN}/aarch64-linux-android34-clang" \
CC_aarch64_linux_android="${NDK_BIN}/aarch64-linux-android34-clang" \
AR_aarch64_linux_android="${NDK_BIN}/llvm-ar" \
SOURCE_DATE_EPOCH=$(git --no-pager log -1 --format=%ct) \
RUSTFLAGS="--remap-path-prefix ${APPCORE}=/979/2fa" \
cargo build --locked --release --target aarch64-linux-android --manifest-path "${APPCORE}/Cargo.toml"

mkdir -p "${APPCORE}/target/jniLibs/arm64-v8a"
cp "${APPCORE}/target/aarch64-linux-android/release/libappcore.so" "${APPCORE}/target/jniLibs/arm64-v8a/"