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
  3. capture before/after D-pad screenshots
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

"$ADB" -s "$ANDROID_SERIAL" shell am start -n com.couchindex.app/.MainActivity >/dev/null
sleep 1

focus_before="$out_dir/focus-before.txt"
focus_after="$out_dir/focus-after.txt"
home_screenshot="$out_dir/home.png"
dpad_screenshot="$out_dir/after-dpad.png"

"$ADB" -s "$ANDROID_SERIAL" shell dumpsys window > "$focus_before"
if ! grep -q "com.couchindex.app" "$focus_before"; then
  echo "CouchIndex is not focused. See $focus_before" >&2
  exit 1
fi

"$ADB" -s "$ANDROID_SERIAL" exec-out screencap -p > "$home_screenshot"

"$ADB" -s "$ANDROID_SERIAL" shell input keyevent KEYCODE_DPAD_DOWN
sleep 0.2
"$ADB" -s "$ANDROID_SERIAL" shell input keyevent KEYCODE_DPAD_UP
sleep 0.2
"$ADB" -s "$ANDROID_SERIAL" shell input keyevent KEYCODE_DPAD_RIGHT
sleep 0.2
"$ADB" -s "$ANDROID_SERIAL" shell input keyevent KEYCODE_DPAD_LEFT
sleep 0.5

"$ADB" -s "$ANDROID_SERIAL" shell dumpsys window > "$focus_after"
if ! grep -q "com.couchindex.app" "$focus_after"; then
  echo "CouchIndex lost focus after D-pad input. See $focus_after" >&2
  exit 1
fi

"$ADB" -s "$ANDROID_SERIAL" exec-out screencap -p > "$dpad_screenshot"

echo "TV smoke test passed."
echo "Screenshots:"
echo "  $home_screenshot"
echo "  $dpad_screenshot"
