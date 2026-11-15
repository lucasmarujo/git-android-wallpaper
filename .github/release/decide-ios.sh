#!/usr/bin/env bash
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=/dev/null
. "${IOS_CONF:-$HERE/ios.conf}"
: "${IOS_INCLUDE:?set IOS_INCLUDE in ios.conf}" "${EVENT:?}" "${REF_TYPE:?}"
IOS_IGNORE=${IOS_IGNORE:-}
FORCE_IOS=${FORCE_IOS:-false}
NO_COMMIT=0000000000000000000000000000000000000000
OUTSIDE_THE_BINARY='(^|/)(__tests__|tests?|[A-Za-z]*Tests|coverage|docs?|android|\.github|\.claude)/|\.(md|test\.[cm]?[jt]sx?|spec\.[cm]?[jt]sx?|gradle|gradle\.kts)$|(^|/)(LICENSE|jest\.config\.[cm]?[jt]s|eslint\.config\.[cm]?[jt]s|\.prettierrc[^/]*|\.prettierignore|\.editorconfig|\.gitignore|\.gitattributes)$'

decision() {
  echo "ios_affected=$1"
  echo "ios_reason=$2"
  exit 0
}

goes_into_the_binary() {
  grep -qE "$IOS_INCLUDE" <<< "$1" || return 1
  grep -qE "$OUTSIDE_THE_BINARY" <<< "$1" && return 1
  if [ -n "$IOS_IGNORE" ] && grep -qE "$IOS_IGNORE" <<< "$1"; then
    return 1
  fi
  return 0
}

if [ "$REF_TYPE" = tag ]; then
  decision true "version tag"
fi

case "$EVENT" in
  push) ;;
  workflow_dispatch)
    if [ "$FORCE_IOS" = true ]; then
      decision true "iOS build forced on the manual dispatch"
    fi
    decision false "manual dispatch without forcing iOS"
    ;;
  *) decision false "the $EVENT event does not publish" ;;
esac

if [ -z "${BEFORE:-}" ] || [ "$BEFORE" = "$NO_COMMIT" ] || ! files=$(git diff --name-only "$BEFORE" "${AFTER:-HEAD}" 2>/dev/null); then
  decision true "no previous commit to compare with"
fi

while IFS= read -r file; do
  if [ -n "$file" ] && goes_into_the_binary "$file"; then
    decision true "$file changed"
  fi
done <<< "$files"

decision false "no file that goes into the iOS binary changed in this push"
