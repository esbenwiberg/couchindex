#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

require_signed=0

usage() {
  cat <<EOF
Usage: scripts/validate-release.sh [--require-signed]

Builds, tests, lints and inspects the release Android App Bundle. Pass
--require-signed before a Play upload to enforce upload-key signing.
EOF
}

while (( "$#" )); do
  case "$1" in
    --require-signed)
      require_signed=1
      shift
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

require_file "$REPO_ROOT/gradlew"
require_executable "$JAVA_HOME/bin/jarsigner"

if (( require_signed )) &&
  [[ -z "${COUCHINDEX_UPLOAD_STORE_FILE:-}" ]] &&
  [[ -z "${COUCHINDEX_UPLOAD_STORE_PASSWORD:-}" ]] &&
  [[ -z "${COUCHINDEX_UPLOAD_KEY_ALIAS:-}" ]] &&
  [[ -z "${COUCHINDEX_UPLOAD_KEY_PASSWORD:-}" ]]; then
  require_executable /usr/bin/security
  keychain_service="com.couchindex.upload-keystore"
  key_alias="couchindex-upload"
  keychain_password="$(/usr/bin/security find-generic-password -a "$key_alias" -s "$keychain_service" -w)"
  export COUCHINDEX_UPLOAD_STORE_FILE="$HOME/.android/couchindex/couchindex-upload.p12"
  export COUCHINDEX_UPLOAD_STORE_PASSWORD="$keychain_password"
  export COUCHINDEX_UPLOAD_KEY_ALIAS="$key_alias"
  export COUCHINDEX_UPLOAD_KEY_PASSWORD="$keychain_password"
  unset keychain_password
fi

"$REPO_ROOT/gradlew" \
  :core:test \
  :app:test \
  :app:lintRelease \
  :app:bundleRelease \
  :app:releaseSigningStatus \
  --no-daemon

bundle="$REPO_ROOT/app/build/outputs/bundle/release/app-release.aab"
banner="$REPO_ROOT/app/src/main/res/drawable-xhdpi/couchindex_banner.png"
banner_source="$REPO_ROOT/design/couchindex_banner.svg"

require_file "$bundle"
require_file "$banner"
require_file "$banner_source"

unzip -tq "$bundle" >/dev/null
bundle_entries="$(unzip -Z1 "$bundle")"

for required_entry in \
  base/manifest/AndroidManifest.xml \
  base/lib/arm64-v8a/libandroidx.graphics.path.so \
  base/lib/x86_64/libandroidx.graphics.path.so; do
  if ! grep -qx "$required_entry" <<<"$bundle_entries"; then
    echo "Release bundle is missing $required_entry" >&2
    exit 1
  fi
done

if command -v sips >/dev/null 2>&1; then
  banner_width="$(sips -g pixelWidth "$banner" | awk '/pixelWidth/ {print $2}')"
  banner_height="$(sips -g pixelHeight "$banner" | awk '/pixelHeight/ {print $2}')"
  if [[ "$banner_width" != "320" || "$banner_height" != "180" ]]; then
    echo "TV banner must be 320x180; found ${banner_width}x${banner_height}." >&2
    exit 1
  fi
fi

if ! grep -q '>CouchIndex</text>' "$banner_source"; then
  echo "TV banner source must contain the CouchIndex name." >&2
  exit 1
fi

signature_report="$("$JAVA_HOME/bin/jarsigner" -verify -verbose -certs "$bundle" 2>&1 || true)"
if grep -q "jar is unsigned" <<<"$signature_report"; then
  bundle_is_signed=0
elif grep -q "jar verified" <<<"$signature_report"; then
  bundle_is_signed=1
else
  echo "Unable to verify the release bundle signature state." >&2
  exit 1
fi

if (( require_signed && ! bundle_is_signed )); then
  echo "Release bundle is unsigned. Configure all COUCHINDEX_UPLOAD_* values." >&2
  exit 1
fi

echo "Release bundle validated: $bundle"
if (( ! bundle_is_signed )); then
  echo "Signing: unsigned local artifact"
else
  echo "Signing: verified"
fi
