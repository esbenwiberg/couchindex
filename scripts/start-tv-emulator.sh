#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

headless=0
wipe_data=0

usage() {
  cat <<EOF
Usage: scripts/start-tv-emulator.sh [--headless] [--wipe-data]

Starts the CouchIndex Google TV emulator and waits for Android to finish booting.

Environment overrides:
  ANDROID_SDK_ROOT  Android SDK path
  AVD_NAME          AVD name, default: CouchIndex_Google_TV
  ANDROID_SERIAL    adb serial, default: emulator-5554
EOF
}

for arg in "$@"; do
  case "$arg" in
    --headless)
      headless=1
      ;;
    --wipe-data)
      wipe_data=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

require_executable "$ADB"
require_executable "$EMULATOR"

if [[ "$(boot_completed)" == "1" ]]; then
  echo "TV emulator already booted: $ANDROID_SERIAL"
  exit 0
fi

if [[ "$(device_state)" == "device" ]]; then
  echo "TV emulator is running; waiting for boot: $ANDROID_SERIAL"
  wait_for_tv_emulator 180
else
  mkdir -p "$REPO_ROOT/build/emulator"
  log_file="$REPO_ROOT/build/emulator/${AVD_NAME}.log"
  pid_file="$REPO_ROOT/build/emulator/${AVD_NAME}.pid"

  emulator_args=(
    -avd "$AVD_NAME"
    -no-audio
    -no-boot-anim
    -gpu swiftshader_indirect
    -no-snapshot
  )

  if (( headless )); then
    emulator_args+=(-no-window)
  fi

  if (( wipe_data )); then
    emulator_args+=(-wipe-data)
  fi

  "$EMULATOR" "${emulator_args[@]}" >"$log_file" 2>&1 &
  emulator_pid=$!
  echo "$emulator_pid" > "$pid_file"

  echo "Started $AVD_NAME as $ANDROID_SERIAL; log: $log_file"
  wait_for_tv_emulator 180
fi

adb_shell settings put system screen_off_timeout 2147483647 >/dev/null || true
echo "TV emulator ready: $ANDROID_SERIAL"
