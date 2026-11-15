#!/usr/bin/env bash
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

failures=0

check() {
  local description=$1 expected=$2 actual=$3
  if [ "$expected" = "$actual" ]; then
    echo "  ok     $description"
  else
    echo "  FAILED $description"
    echo "    expected: $expected"
    echo "    actual:   $actual"
    failures=$((failures + 1))
  fi
}

plan() {
  env -i PATH="$PATH" EVENT="$1" REF_TYPE="$2" REF_NAME="$3" VERSION="$4" RUN_NUMBER=7 \
    VERSION_CODE_OFFSET=1000 MANUAL_PUBLISH="${5:-false}" TAG_PREFIX="${TAG_PREFIX:-v}" \
    ${IOS_VERSION:+IOS_VERSION="$IOS_VERSION"} bash "$HERE/plan.sh" 2>&1 | tr '\n' ' '
}

echo "plan.sh"
check "push to the main branch publishes to internal testing" \
  "version=1.2.0 version_code=1007 track=internal publish=true " "$(plan push branch main 1.2.0)"
check "tag equal to the version publishes to production" \
  "version=1.2.0 version_code=1007 track=production publish=true " "$(plan push tag v1.2.0 1.2.0)"
check "dispatch without publishing only builds the AAB" \
  "version=1.2.0 version_code=1007 track=internal publish=false " "$(plan workflow_dispatch branch main 1.2.0 false)"
check "dispatch with publishing goes to internal testing" \
  "version=1.2.0 version_code=1007 track=internal publish=true " "$(plan workflow_dispatch branch main 1.2.0 true)"
check "pull request never publishes" \
  "version=1.2.0 version_code=1007 track=internal publish=false " "$(plan pull_request branch 3/merge 1.2.0)"
check "configurable tag prefix" \
  "version=1.0.3 version_code=1007 track=production publish=true " "$(TAG_PREFIX=mobile-v plan push tag mobile-v1.0.3 1.0.3)"

set +e
output=$(plan push tag v1.3.0 1.2.0)
status=$?
set -e
check "tag different from the version fails" "1" "$status"
check "tag different from the version explains the error" "yes" "$(grep -q 'does not match version 1.2.0' <<< "$output" && echo yes || echo no)"

set +e
output=$(IOS_VERSION=1.0.0 plan push tag v1.2.0 1.2.0)
status=$?
set -e
check "tag different from the iOS version fails" "1" "$status"

echo "release-notes.sh"
repo=$TMP/repo
git init -q "$repo"
cd "$repo"
git config user.email test@example.com
git config user.name test
seconds=0
commit() {
  seconds=$((seconds + 1))
  GIT_AUTHOR_DATE="2026-01-01T00:00:${seconds}Z" GIT_COMMITTER_DATE="2026-01-01T00:00:${seconds}Z" \
    git commit -q --allow-empty -m "$1"
}

commit "first delivery"
commit "second delivery"
check "without a previous tag it lists the commits" "$(printf -- '- second delivery\n- first delivery')" \
  "$(bash "$HERE/release-notes.sh" 500 fallback)"

git tag v1.0.0
check "without commits after the tag it uses the fallback note" "fallback" "$(bash "$HERE/release-notes.sh" 500 fallback)"

commit "fix the login"
git checkout -q -b side
commit "tweak on side"
git checkout -q -
seconds=$((seconds + 1))
GIT_COMMITTER_DATE="2026-01-01T00:00:${seconds}Z" git merge -q --no-ff side -m "Merge side"
check "lists only what came after the previous tag, without merges" \
  "$(printf -- '- tweak on side\n- fix the login')" "$(bash "$HERE/release-notes.sh" 500 fallback)"

git tag v1.1.0
check "lightweight tag uses the commits since the previous tag" \
  "$(printf -- '- tweak on side\n- fix the login')" "$(bash "$HERE/release-notes.sh" 500 fallback v1.1.0)"

git tag -a v1.1.1 -m "New plans screen" -m "Faster to open."
check "annotated tag uses the tag message" \
  "$(printf 'New plans screen\n\nFaster to open.')" "$(bash "$HERE/release-notes.sh" 500 fallback v1.1.1)"

commit "a"
commit "bb"
commit "ccc"
check "cuts at a whole line within the limit" "$(printf -- '- ccc\n- bb')" "$(bash "$HERE/release-notes.sh" 11 fallback)"

git tag mobile-v2.0.0
commit "after mobile"
check "respects the tag prefix" "- after mobile" "$(TAG_PREFIX=mobile-v bash "$HERE/release-notes.sh" 500 fallback)"

cd "$TMP"

echo "check-aab.sh"
keytool -genkeypair -keystore "$TMP/upload.jks" -storepass test-password -keypass test-password -alias upload \
  -keyalg RSA -keysize 2048 -validity 1 -dname CN=test > /dev/null 2>&1
keytool -genkeypair -keystore "$TMP/other.jks" -storepass test-password -keypass test-password -alias other \
  -keyalg RSA -keysize 2048 -validity 1 -dname CN=other > /dev/null 2>&1

make_module() {
  local module=$1 version_code=$2
  mkdir -p "$module/build/outputs/bundle/release" "$module/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage"
  echo "<manifest android:versionCode=\"$version_code\" />" \
    > "$module/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml"
  echo content > "$TMP/content.txt"
  (cd "$TMP" && jar cf "$module/build/outputs/bundle/release/app-release.aab" content.txt)
}

check_aab() {
  env ANDROID_VERSION_CODE="$2" ANDROID_KEYSTORE_PASSWORD=test-password ANDROID_KEY_ALIAS=upload \
    bash "$HERE/check-aab.sh" "$1" "$TMP/upload.jks" 2>&1
}

make_module "$TMP/signed" 1007
jarsigner -keystore "$TMP/upload.jks" -storepass test-password "$TMP/signed/build/outputs/bundle/release/app-release.aab" upload > /dev/null 2>&1
check "AAB with the upload key and the right versionCode passes" \
  "aab=$TMP/signed/build/outputs/bundle/release/app-release.aab" "$(check_aab "$TMP/signed" 1007)"

set +e
output=$(check_aab "$TMP/signed" 1008)
status=$?
set -e
check "different versionCode fails" "1" "$status"
check "different versionCode explains the error" "yes" "$(grep -q 'versionCode 1008' <<< "$output" && echo yes || echo no)"

make_module "$TMP/unsigned" 1007
set +e
output=$(check_aab "$TMP/unsigned" 1007)
status=$?
set -e
check "unsigned AAB fails" "1" "$status"

make_module "$TMP/other-key" 1007
jarsigner -keystore "$TMP/other.jks" -storepass test-password "$TMP/other-key/build/outputs/bundle/release/app-release.aab" other > /dev/null 2>&1
set +e
output=$(check_aab "$TMP/other-key" 1007)
status=$?
set -e
check "AAB signed with another key fails" "1" "$status"
check "another key explains the error" "yes" "$(grep -q 'upload key' <<< "$output" && echo yes || echo no)"

echo "app-store-connect-key.sh"
mkdir -p "$TMP/asc"
echo '{"submit":{"production":{"android":{"track":"internal"},"ios":{"ascAppId":"123","ascApiKeyPath":"./old.p8"}}}}' > "$TMP/asc/eas.json"
APP_STORE_CONNECT_API_KEY_BASE64=$(printf 'key-content' | base64) APP_STORE_CONNECT_API_KEY_ID=ABC123 \
  APP_STORE_CONNECT_API_ISSUER_ID=issuer bash "$HERE/app-store-connect-key.sh" "$TMP/asc/eas.json" "$TMP/asc"
check "writes the decoded key" "key-content" "$(cat "$TMP/asc/asc-api-key.p8")"
check "points the submit profile to the key" "$TMP/asc/asc-api-key.p8 ABC123 issuer 123" \
  "$(jq -r '.submit.production.ios | "\(.ascApiKeyPath) \(.ascApiKeyId) \(.ascApiKeyIssuerId) \(.ascAppId)"' "$TMP/asc/eas.json")"
check "keeps the rest of eas.json" "internal" "$(jq -r .submit.production.android.track "$TMP/asc/eas.json")"
check "writes the key JSON for fastlane" "ABC123 issuer key-content" \
  "$(jq -r '"\(.key_id) \(.issuer_id) \(.key)"' "$TMP/asc/asc-api-key.json")"

echo "ios-credentials.sh"
mkdir -p "$TMP/bundle/credentials/ios" "$TMP/app-ios" "$TMP/tmp-ios"
echo p12 > "$TMP/bundle/credentials/ios/dist.p12"
echo profile > "$TMP/bundle/credentials/ios/store.mobileprovision"
echo '{"ios":{"App":{"provisioningProfilePath":"credentials/ios/store.mobileprovision","distributionCertificate":{"path":"credentials/ios/dist.p12","password":"secret"}}}}' \
  > "$TMP/bundle/credentials.json"
echo '{"android":{"keystore":{"keystorePath":"upload.jks"}},"ios":{"old":true}}' > "$TMP/app-ios/credentials.json"
IOS_CREDENTIALS_BASE64=$(tar -czf - -C "$TMP/bundle" . | base64 -w0) bash "$HERE/ios-credentials.sh" "$TMP/app-ios" "$TMP/tmp-ios"
check "replaces only the iOS part and makes paths absolute" \
  "upload.jks $TMP/tmp-ios/ios-credentials/credentials/ios/store.mobileprovision $TMP/tmp-ios/ios-credentials/credentials/ios/dist.p12 secret null" \
  "$(jq -r '"\(.android.keystore.keystorePath) \(.ios.App.provisioningProfilePath) \(.ios.App.distributionCertificate.path) \(.ios.App.distributionCertificate.password) \(.ios.old)"' "$TMP/app-ios/credentials.json")"
check "extracts the files" "p12" "$(cat "$TMP/tmp-ios/ios-credentials/credentials/ios/dist.p12")"
rm "$TMP/app-ios/credentials.json"
IOS_CREDENTIALS_BASE64=$(tar -czf - -C "$TMP/bundle" . | base64 -w0) bash "$HERE/ios-credentials.sh" "$TMP/app-ios" "$TMP/tmp-ios"
check "creates credentials.json when missing" "secret" "$(jq -r .ios.App.distributionCertificate.password "$TMP/app-ios/credentials.json")"

if [ "$failures" -gt 0 ]; then
  echo "$failures test(s) failed"
  exit 1
fi
echo "all tests passed"
