#!/bin/bash

rustup target add aarch64-linux-android

pushd ./appcore

cargo build --target aarch64-linux-android --release --target-dir ./target

mkdir -p target/jniLibs/arm64-v8a

cp target/aarch64-linux-android/release/libappcore.so target/jniLibs/arm64-v8a/

popd

echo "Done"