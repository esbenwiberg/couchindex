#!/usr/bin/env bash
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/android-env.sh"

key_dir="${COUCHINDEX_UPLOAD_KEY_DIR:-$HOME/.android/couchindex}"
key_path="$key_dir/couchindex-upload.p12"
key_alias="couchindex-upload"
keychain_service="com.couchindex.upload-keystore"
keytool="$JAVA_HOME/bin/keytool"

require_executable "$keytool"
require_executable /usr/bin/openssl
require_executable /usr/bin/security

if [[ -e "$key_path" ]]; then
  echo "Upload keystore already exists: $key_path" >&2
  exit 1
fi

if /usr/bin/security find-generic-password -a "$key_alias" -s "$keychain_service" >/dev/null 2>&1; then
  echo "Upload-key password already exists in Keychain; refusing to replace it." >&2
  exit 1
fi

mkdir -p "$key_dir"
password="$(/usr/bin/openssl rand -hex 32)"

COUCHINDEX_GENERATED_UPLOAD_PASSWORD="$password" "$keytool" \
  -genkeypair \
  -alias "$key_alias" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=CouchIndex Upload Key" \
  -storetype PKCS12 \
  -keystore "$key_path" \
  -storepass:env COUCHINDEX_GENERATED_UPLOAD_PASSWORD \
  -keypass:env COUCHINDEX_GENERATED_UPLOAD_PASSWORD \
  >/dev/null
chmod 600 "$key_path"

if ! /usr/bin/security add-generic-password \
  -a "$key_alias" \
  -s "$keychain_service" \
  -l "CouchIndex Play upload keystore" \
  -w "$password" \
  -T /usr/bin/security \
  >/dev/null; then
  rm -f "$key_path"
  echo "Unable to save the upload-key password in macOS Keychain." >&2
  exit 1
fi

unset password

echo "CouchIndex upload key created."
echo "Keystore: $key_path"
echo "Alias: $key_alias"
echo "Password: stored in macOS Keychain service $keychain_service"
