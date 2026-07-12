#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

skip_build=0

usage() {
  cat <<EOF
Usage: scripts/install-tv-debug.sh [--skip-build]

Builds the debug APK, installs it on the Google TV emulator, and launches CouchIndex.
EOF
}

for arg in "$@"; do
  case "$arg" in
    --skip-build)
      skip_build=1
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
require_file "$REPO_ROOT/gradlew"

if [[ "$(boot_completed)" != "1" ]]; then
  "$SCRIPT_DIR/start-tv-emulator.sh"
fi

if (( ! skip_build )); then
  "$REPO_ROOT/gradlew" :app:assembleDebug
fi

apk_path="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
require_file "$apk_path"

"$ADB" -s "$ANDROID_SERIAL" install -r "$apk_path"
"$ADB" -s "$ANDROID_SERIAL" shell am start -n com.couchindex.app/.MainActivity

echo "CouchIndex launched on $ANDROID_SERIAL"
