#!/usr/bin/env bash
set -euo pipefail

: "${EVENT:?}" "${REF_TYPE:?}" "${REF_NAME:?}" "${VERSION:?}" "${RUN_NUMBER:?}" "${VERSION_CODE_OFFSET:?}"
TAG_PREFIX=${TAG_PREFIX:-v}
MANUAL_PUBLISH=${MANUAL_PUBLISH:-false}
IOS_VERSION=${IOS_VERSION:-$VERSION}

track=internal
if [ "$REF_TYPE" = tag ]; then
  tag_version=${REF_NAME#"$TAG_PREFIX"}
  for project_version in "$VERSION" "$IOS_VERSION"; do
    if [ "$tag_version" != "$project_version" ]; then
      echo "::error::Tag $REF_NAME does not match version $project_version of the project. Raise the version, merge it into the main branch and create the tag again." >&2
      exit 1
    fi
  done
  track=production
fi

case "$EVENT" in
  push) publish=true ;;
  workflow_dispatch) publish=$MANUAL_PUBLISH ;;
  *) publish=false ;;
esac

echo "version=$VERSION"
echo "version_code=$((VERSION_CODE_OFFSET + RUN_NUMBER))"
echo "track=$track"
echo "publish=$publish"
