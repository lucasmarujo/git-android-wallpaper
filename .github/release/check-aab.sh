#!/usr/bin/env bash
set -euo pipefail

module=${1:?app module directory}
keystore=${2:?upload keystore}
: "${ANDROID_VERSION_CODE:?}" "${ANDROID_KEYSTORE_PASSWORD:?}" "${ANDROID_KEY_ALIAS:?}"

fail() {
  echo "::error::$1" >&2
  exit 1
}

aab="$module/build/outputs/bundle/release/app-release.aab"
[ -f "$aab" ] || fail "AAB not found at $aab."

fingerprint() { sed -n 's/.*SHA256: *//p' | head -n 1; }
expected=$(keytool -list -v -keystore "$keystore" -storepass "$ANDROID_KEYSTORE_PASSWORD" -alias "$ANDROID_KEY_ALIAS" | fingerprint)
actual=$(keytool -printcert -jarfile "$aab" | fingerprint)
[ -n "$expected" ] && [ "$expected" = "$actual" ] || fail "The AAB is not signed with the upload key."
jarsigner -verify "$aab" > /dev/null || fail "The AAB signature does not verify."

manifest=$(find "$module/build/intermediates" -path '*packaged_manifests*release*' -name AndroidManifest.xml | head -n 1)
[ -n "$manifest" ] || fail "Packaged release manifest not found."
grep -q "android:versionCode=\"$ANDROID_VERSION_CODE\"" "$manifest" || fail "The AAB was not built with versionCode $ANDROID_VERSION_CODE."

echo "aab=$aab"
