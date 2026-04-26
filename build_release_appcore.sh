#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$SCRIPT_DIR/.env" ]; then
    set -a && source "$SCRIPT_DIR/.env" && set +a
else
    echo "Error: .env not found at $SCRIPT_DIR/.env"
    exit 1
fi

mkdir -p "$SCRIPT_DIR/appcore/.cargo"
cat > "$SCRIPT_DIR/appcore/.cargo/config.toml" << EOF
[target.aarch64-linux-android]
ar = "$AR_AARCH64_LINUX_ANDROID"
linker = "$LINKER_AARCH64_LINUX_ANDROID"
rustflags = [
  "-C", "target-feature=+neon,+mte"
]
EOF

rustup update stable
rustup target add aarch64-linux-android
pushd "$SCRIPT_DIR/appcore"

cargo clean --target-dir ./target
cargo build --target aarch64-linux-android --release --target-dir ./target

mkdir -p target/jniLibs/arm64-v8a
cp target/aarch64-linux-android/release/libappcore.so target/jniLibs/arm64-v8a/

popd
echo "Done"