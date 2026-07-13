# Development

This project is a native Android TV app. Browser tooling is useful for docs and release
pages, but runtime validation happens through Android Studio, the emulator and `adb`.

## Local toolchain

- Android Studio with its bundled JDK 17 runtime
- Android SDK API 37
- Android SDK Build Tools 36.0.0
- Google TV emulator image
- The `CouchIndex_Google_TV` AVD

The helper scripts default to Android Studio's bundled JDK at:

```text
/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

Override paths with `JAVA_HOME`, `ANDROID_SDK_ROOT`, `AVD_NAME` or `ANDROID_SERIAL`
when needed.

## Common commands

Start the visible Google TV emulator:

```sh
./scripts/start-tv-emulator.sh
```

Start it without a window for automated checks:

```sh
./scripts/start-tv-emulator.sh --headless
```

Build, install and launch the debug app:

```sh
./scripts/install-tv-debug.sh
```

Run the local TV smoke check:

```sh
./scripts/tv-smoke.sh
```

Smoke screenshots are written under `build/tv-smoke/`, which is ignored by git.

## Release bundle

Build, test, lint and inspect an unsigned local Android App Bundle with:

```sh
./scripts/validate-release.sh
```

Release signing is optional for local validation. Before a Play upload, create a
recoverable upload key and provide all four values through ignored `local.properties`
entries or environment variables:

```properties
COUCHINDEX_UPLOAD_STORE_FILE=/absolute/path/to/couchindex-upload.jks
COUCHINDEX_UPLOAD_STORE_PASSWORD=your_store_password
COUCHINDEX_UPLOAD_KEY_ALIAS=couchindex-upload
COUCHINDEX_UPLOAD_KEY_PASSWORD=your_key_password
```

Then enforce signature verification:

```sh
./scripts/validate-release.sh --require-signed
```

The release bundle is written to
`app/build/outputs/bundle/release/app-release.aab`. Never commit a keystore or its
credentials.

## Catalogue cache

The last successful live provider directory and enriched catalogue are stored atomically in the app-private file
`files/catalogue-snapshot-v1.json`. A debug emulator can verify it after a successful refresh with:

```sh
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  run-as com.couchindex.app ls -l files/catalogue-snapshot-v1.json
```

Malformed snapshots and unsupported versions are ignored. Cached data is labeled stale after 24 hours.

## D-pad input

The emulator's Extended Controls directional pad can be inconsistent on macOS. The
most reliable path is serial-targeted `adb` input:

```sh
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell input keyevent KEYCODE_DPAD_UP
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell input keyevent KEYCODE_DPAD_DOWN
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell input keyevent KEYCODE_DPAD_LEFT
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell input keyevent KEYCODE_DPAD_RIGHT
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell input keyevent KEYCODE_DPAD_CENTER
```

If the emulator is stuck in Android TV Settings or another system screen, relaunch
CouchIndex directly:

```sh
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am start -n com.couchindex.app/.MainActivity
```

Check the current foreground app:

```sh
/Users/ewi/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell dumpsys window | rg 'mCurrentFocus|mFocusedApp'
```
