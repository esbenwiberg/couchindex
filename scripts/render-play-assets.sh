#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"

if [[ ! -x "$CHROME" ]]; then
  echo "Missing Chrome executable: $CHROME" >&2
  exit 1
fi

render_svg() {
  local source="$1"
  local destination="$2"
  local width="$3"
  local height="$4"
  local profile_name="$5"
  local background_flag="${6:-}"
  local render_dir="$REPO_ROOT/build/play-asset-render/$profile_name-$$"
  local rendered="$render_dir/rendered.png"
  local chrome_pid
  local attempt

  mkdir -p "$(dirname "$destination")" "$render_dir"
  "$CHROME" \
    --headless \
    --disable-gpu \
    --disable-background-networking \
    --no-first-run \
    --hide-scrollbars \
    --force-device-scale-factor=1 \
    --user-data-dir="$render_dir/profile" \
    --window-size="$width,$height" \
    ${background_flag:+"$background_flag"} \
    --screenshot="$rendered" \
    "file://$source" &
  chrome_pid=$!

  for attempt in {1..60}; do
    if [[ -s "$rendered" ]]; then
      sleep 1
      kill "$chrome_pid" 2>/dev/null || true
      wait "$chrome_pid" 2>/dev/null || true
      mv "$rendered" "$destination"
      return
    fi
    sleep 0.5
  done

  kill "$chrome_pid" 2>/dev/null || true
  wait "$chrome_pid" 2>/dev/null || true
  echo "Timed out rendering $source" >&2
  exit 1
}

render_svg \
  "$REPO_ROOT/design/couchindex_banner.svg" \
  "$REPO_ROOT/app/src/main/res/drawable-xhdpi/couchindex_banner.png" \
  320 180 launcher
render_svg \
  "$REPO_ROOT/design/play-store/couchindex_icon_512.svg" \
  "$REPO_ROOT/design/play-store/couchindex_icon_512.png" \
  512 512 icon --default-background-color=00000000
render_svg \
  "$REPO_ROOT/design/play-store/couchindex_feature_1024x500.svg" \
  "$REPO_ROOT/design/play-store/couchindex_feature_1024x500.png" \
  1024 500 feature
render_svg \
  "$REPO_ROOT/design/play-store/couchindex_tv_banner_1280x720.svg" \
  "$REPO_ROOT/design/play-store/couchindex_tv_banner_1280x720.png" \
  1280 720 tv-banner

validate_image() {
  local path="$1"
  local expected_width="$2"
  local expected_height="$3"
  local expected_alpha="$4"
  local properties

  properties="$(sips -g pixelWidth -g pixelHeight -g hasAlpha "$path")"
  grep -q "pixelWidth: $expected_width" <<<"$properties"
  grep -q "pixelHeight: $expected_height" <<<"$properties"
  grep -q "hasAlpha: $expected_alpha" <<<"$properties"
}

validate_image "$REPO_ROOT/app/src/main/res/drawable-xhdpi/couchindex_banner.png" 320 180 no
validate_image "$REPO_ROOT/design/play-store/couchindex_icon_512.png" 512 512 yes
validate_image "$REPO_ROOT/design/play-store/couchindex_feature_1024x500.png" 1024 500 no
validate_image "$REPO_ROOT/design/play-store/couchindex_tv_banner_1280x720.png" 1280 720 no

echo "Play graphics rendered and validated."
