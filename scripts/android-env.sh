#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export AVD_NAME="${AVD_NAME:-CouchIndex_Google_TV}"
export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$REPO_ROOT/.gradle}"

ADB="${ADB:-$ANDROID_SDK_ROOT/platform-tools/adb}"
EMULATOR="${EMULATOR:-$ANDROID_SDK_ROOT/emulator/emulator}"

DEFAULT_ANDROID_STUDIO_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [[ -z "${JAVA_HOME:-}" && -x "$DEFAULT_ANDROID_STUDIO_JBR/bin/java" ]]; then
  export JAVA_HOME="$DEFAULT_ANDROID_STUDIO_JBR"
fi

require_executable() {
  local path="$1"
  if [[ ! -x "$path" ]]; then
    echo "Missing executable: $path" >&2
    exit 1
  fi
}

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "Missing file: $path" >&2
    exit 1
  fi
}

adb_shell() {
  "$ADB" -s "$ANDROID_SERIAL" shell "$@"
}

device_state() {
  "$ADB" -s "$ANDROID_SERIAL" get-state 2>/dev/null || true
}

boot_completed() {
  adb_shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true
}

wait_for_tv_emulator() {
  local timeout_seconds="${1:-180}"

  "$ADB" -s "$ANDROID_SERIAL" wait-for-device

  local elapsed=0
  while (( elapsed < timeout_seconds )); do
    if [[ "$(boot_completed)" == "1" ]]; then
      return 0
    fi

    sleep 1
    elapsed=$((elapsed + 1))
  done

  echo "Timed out waiting for $ANDROID_SERIAL to finish booting." >&2
  return 1
}
