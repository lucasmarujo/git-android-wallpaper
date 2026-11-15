#!/usr/bin/env bash
set -euo pipefail

limit=${1:?character limit}
fallback=${2:?note used when there are no commits}
tag=${3:-}
prefix=${TAG_PREFIX:-v}

if [ -n "$tag" ] && [ "$(git cat-file -t "refs/tags/$tag" 2>/dev/null)" = tag ]; then
  notes=$(git for-each-ref "refs/tags/$tag" --format='%(contents:subject)%0a%0a%(contents:body)')
else
  arguments=(--match "${prefix}[0-9]*.[0-9]*.[0-9]*")
  [ -z "$tag" ] || arguments+=(--exclude "$tag")
  previous=$(git describe --tags --abbrev=0 "${arguments[@]}" HEAD 2>/dev/null || true)
  if [ -n "$previous" ]; then
    range=("$previous..HEAD")
  else
    range=(-20 HEAD)
  fi
  notes=$(git log --no-merges --pretty='- %s' "${range[@]}")
fi

notes=$(printf '%s\n' "$notes" | awk -v limit="$limit" '{ if (total + length($0) + 1 > limit) exit; print; total += length($0) + 1 }')

if [ -z "${notes//[[:space:]]/}" ]; then
  notes=$fallback
fi

printf '%s\n' "$notes"
