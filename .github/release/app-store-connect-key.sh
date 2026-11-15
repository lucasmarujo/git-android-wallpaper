#!/usr/bin/env bash
set -euo pipefail

eas_json=${1:?path to eas.json}
destination=${2:?temporary directory}
: "${APP_STORE_CONNECT_API_KEY_BASE64:?}" "${APP_STORE_CONNECT_API_KEY_ID:?}" "${APP_STORE_CONNECT_API_ISSUER_ID:?}"

key="$destination/asc-api-key.p8"
printf '%s' "$APP_STORE_CONNECT_API_KEY_BASE64" | base64 --decode > "$key"

jq --arg path "$key" --arg id "$APP_STORE_CONNECT_API_KEY_ID" --arg issuer "$APP_STORE_CONNECT_API_ISSUER_ID" \
  '.submit.production.ios += {ascApiKeyPath: $path, ascApiKeyId: $id, ascApiKeyIssuerId: $issuer}' \
  "$eas_json" > "$destination/eas.json"
mv "$destination/eas.json" "$eas_json"

jq -n --arg key_id "$APP_STORE_CONNECT_API_KEY_ID" --arg issuer_id "$APP_STORE_CONNECT_API_ISSUER_ID" --rawfile key "$key" \
  '{key_id: $key_id, issuer_id: $issuer_id, key: $key, in_house: false}' > "$destination/asc-api-key.json"
