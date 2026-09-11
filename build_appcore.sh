#!/usr/bin/env bash
set -euo pipefail

OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"

if [[ "$OS" == "darwin" && "$ARCH" == "arm64" ]]; then
    ARCH="x86_64"
fi

ROOT="$(cd "$(dirname "$0")" && pwd -P)"
ANDROID="${ROOT}/android"
APPCORE="${ROOT}/appcore"
CONFIG_TOML="${APPCORE}/.cargo/config.toml"

MINIMUM_SDK_VERSION="$(sed -n 's/.*minSdk = \([0-9]*\).*/\1/p' "${ANDROID}/app/build.gradle.kts")"
NDK_VERSION="$(sed -n 's/^ndk = "\([^"]*\)".*/\1/p' "${ANDROID}/gradle/libs.versions.toml")"

NDK_BIN="${ANDROID_HOME}/ndk/${NDK_VERSION}/toolchains/llvm/prebuilt/${OS}-${ARCH}/bin"

CARGO_REGISTRY="${CARGO_HOME:-${HOME}/.cargo}/registry"
RUST_SRC="$(rustc --print sysroot)/lib/rustlib/src/rust"
RUSTC_COMMIT="$(rustc -vV | sed -n 's/^commit-hash: \(.*\)/\1/p')"

mkdir -p "${APPCORE}/.cargo"
cat << EOF > "$CONFIG_TOML"
[profile.release]
opt-level = "z"
lto = true
codegen-units = 1
panic = "abort"
strip = true

[target.aarch64-linux-android]
ar = "${NDK_BIN}/llvm-ar"
linker = "${NDK_BIN}/aarch64-linux-android${MINIMUM_SDK_VERSION}-clang"
rustflags = [
    "-C", "link-arg=-Wl,--build-id=none",
    "-C", "link-arg=-Wl,--strip-all",
    "-C", "target-feature=+neon",
    "--remap-path-prefix", "${APPCORE}=",
    "--remap-path-prefix", "${HOME}=",
    "--remap-path-prefix", "${CARGO_REGISTRY}/=",
    "--remap-path-prefix", "${RUST_SRC}=/rustc/${RUSTC_COMMIT}",
    "--remap-path-scope=object"
]
EOF

unset RUSTFLAGS CARGO_ENCODED_RUSTFLAGS CARGO_BUILD_RUSTFLAGS

if [[ ! -f "${APPCORE}/Cargo.lock" ]]; then
    cargo generate-lockfile --quiet --manifest-path "${APPCORE}/Cargo.toml" --config "$CONFIG_TOML"
fi

SOURCE_DATE_EPOCH=$(git --no-pager log -1 --format=%ct) \
cargo build --quiet --locked --release --target aarch64-linux-android --manifest-path "${APPCORE}/Cargo.toml" --config "$CONFIG_TOML"

JNI_LIBS="${APPCORE}/target/jniLibs/arm64-v8a"

mkdir -p "$JNI_LIBS"
cp "${APPCORE}/target/aarch64-linux-android/release/libappcore.so" "$JNI_LIBS/"

"${NDK_BIN}/llvm-objcopy" --remove-section .comment "${JNI_LIBS}/libappcore.so"