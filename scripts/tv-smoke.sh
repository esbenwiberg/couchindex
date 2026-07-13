#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

skip_install=0
out_dir="$REPO_ROOT/build/tv-smoke"

usage() {
  cat <<EOF
Usage: scripts/tv-smoke.sh [--skip-install] [--out-dir DIR]

Runs a small emulator smoke test:
  1. build/install/launch CouchIndex unless --skip-install is passed
  2. verify the app is foregrounded
  3. capture Home and D-pad-reached Settings screenshots
EOF
}

while (( "$#" )); do
  case "$1" in
    --skip-install)
      skip_install=1
      shift
      ;;
    --out-dir)
      if [[ "$#" -lt 2 ]]; then
        echo "--out-dir requires a path" >&2
        exit 2
      fi
      out_dir="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

require_executable "$ADB"

if (( ! skip_install )); then
  "$SCRIPT_DIR/install-tv-debug.sh"
elif [[ "$(boot_completed)" != "1" ]]; then
  "$SCRIPT_DIR/start-tv-emulator.sh"
fi

mkdir -p "$out_dir"

"$ADB" -s "$ANDROID_SERIAL" shell am force-stop com.couchindex.app
"$ADB" -s "$ANDROID_SERIAL" shell am start -n com.couchindex.app/.MainActivity >/dev/null
# Cold-start Compose rendering trails the activity focus event on the headless TV image.
sleep 6

focus_before="$out_dir/focus-before.txt"
focus_after="$out_dir/focus-after.txt"
home_screenshot="$out_dir/home.png"
dpad_screenshot="$out_dir/after-dpad.png"

capture_settled_screenshot() {
  local destination="$1"
  "$ADB" -s "$ANDROID_SERIAL" exec-out screencap -p >/dev/null
  sleep 6
  "$ADB" -s "$ANDROID_SERIAL" exec-out screencap -p > "$destination"
}

"$ADB" -s "$ANDROID_SERIAL" shell dumpsys window > "$focus_before"
if ! grep -q "com.couchindex.app" "$focus_before"; then
  echo "CouchIndex is not focused. See $focus_before" >&2
  exit 1
fi

capture_settled_screenshot "$home_screenshot"

send_key() {
  "$ADB" -s "$ANDROID_SERIAL" shell input keyevent "$1"
  sleep 0.5
}

# Home is initially focused in the destination rail; Settings is three rows below it.
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_CENTER
send_key KEYCODE_DPAD_RIGHT
send_key KEYCODE_DPAD_UP
sleep 6

"$ADB" -s "$ANDROID_SERIAL" shell dumpsys window > "$focus_after"
if ! grep -q "com.couchindex.app" "$focus_after"; then
  echo "CouchIndex lost focus after D-pad input. See $focus_after" >&2
  exit 1
fi

capture_settled_screenshot "$dpad_screenshot"

echo "TV smoke test passed."
echo "Screenshots:"
echo "  $home_screenshot"
echo "  $dpad_screenshot"
