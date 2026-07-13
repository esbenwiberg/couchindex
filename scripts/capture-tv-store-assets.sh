#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

skip_install=0
out_dir="$REPO_ROOT/build/play-store/tv"

usage() {
  cat <<EOF
Usage: scripts/capture-tv-store-assets.sh [--skip-install] [--out-dir DIR]

Captures clean 1920x1080 Home, Browse, Search and Settings screenshots from the
Google TV emulator for Play store preparation.
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

send_key() {
  adb_shell input keyevent "$1"
  sleep 0.6
}

launch_home() {
  adb_shell am force-stop com.couchindex.app
  adb_shell am start -n com.couchindex.app/.MainActivity >/dev/null
  sleep 8
}

capture() {
  local destination="$1"
  "$ADB" -s "$ANDROID_SERIAL" exec-out screencap -p >/dev/null
  sleep 4
  "$ADB" -s "$ANDROID_SERIAL" exec-out screencap -p > "$destination"
}

launch_home
capture "$out_dir/01-home.png"

launch_home
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_CENTER
send_key KEYCODE_DPAD_RIGHT
capture "$out_dir/02-browse.png"

launch_home
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_CENTER
send_key KEYCODE_DPAD_RIGHT
adb_shell input text interstel
sleep 1
adb_shell input text lar
sleep 3
send_key KEYCODE_BACK
capture "$out_dir/03-search.png"

launch_home
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_DOWN
send_key KEYCODE_DPAD_CENTER
capture "$out_dir/04-settings.png"

for screenshot in "$out_dir"/*.png; do
  dimensions="$(sips -g pixelWidth -g pixelHeight "$screenshot" 2>/dev/null)"
  if ! grep -q 'pixelWidth: 1920' <<<"$dimensions" || ! grep -q 'pixelHeight: 1080' <<<"$dimensions"; then
    echo "Unexpected screenshot dimensions: $screenshot" >&2
    exit 1
  fi
done

echo "Android TV store screenshots captured in $out_dir"
