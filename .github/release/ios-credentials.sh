#!/usr/bin/env bash
set -euo pipefail

app_dir=${1:?app directory}
destination=${2:?temporary directory}
: "${IOS_CREDENTIALS_BASE64:?}"

files="$destination/ios-credentials"
mkdir -p "$files"
printf '%s' "$IOS_CREDENTIALS_BASE64" | base64 --decode | tar -xz -C "$files"

ios=$(jq --arg dir "$files" \
  '.ios | walk(if type == "string" and (endswith(".p12") or endswith(".mobileprovision")) then "\($dir)/\(.)" else . end)' \
  "$files/credentials.json")

credentials="$app_dir/credentials.json"
[ -f "$credentials" ] || echo '{}' > "$credentials"
jq --argjson ios "$ios" '.ios = $ios' "$credentials" > "$destination/credentials.json.tmp"
mv "$destination/credentials.json.tmp" "$credentials"
