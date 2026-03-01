## Contributor License Agreement

This project requires a Contributor License Agreement (CLA).

By submitting a contribution, you confirm that you have the right to do so
and that you grant the project maintainer the rights described in the CLA.

## Submitting a Pull Request

Before starting work, please note:
- Contributors might be working on changes that you would like to make. Join our chat rooms and coordinate with them to avoid duplicate work.
- A single commit is allowed per pull request. This helps code reviews and keeps the history clean.
- Commit messages should be short and descriptive. You can always add more information in the description.
- The maintainer may request changes, please respond promptly.

## Compiling appcore ARM64

Add `appcore/.cargo/config.toml` with your Android SDK paths:

```toml
[target.aarch64-linux-android]
ar = "<ANDROID_SDK>/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar"
linker = "<ANDROID_SDK>/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android34-clang" # Android 14 (SDK 34)
rustflags = ["-C", "target-feature=+neon"]
```

then run the `build_appcore.sh`.