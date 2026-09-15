#!/usr/bin/env bash
set -euo pipefail

revision="68d10a4b21150cae5e1ebbd223eed289cf32d90c"
archive_sha256="6596f675e58dafb5b6bd58d003e0f407d88e4bbccf8477c1ee2039a6faefab55"
work_dir="$(mktemp -d)"
archive="$work_dir/hanzi-writer-data.tar.gz"

curl --fail --location --retry 4 --silent --show-error \
  --output "$archive" \
  "https://github.com/chanind/hanzi-writer-data/archive/$revision.tar.gz"
printf '%s  %s\n' "$archive_sha256" "$archive" | sha256sum --check --strict
tar -xzf "$archive" -C "$work_dir"

python3 tools/build_stroke_database.py \
  --data-dir "$work_dir/hanzi-writer-data-$revision/data" \
  --output app/src/main/assets/databases/strokes.db \
  --revision "$revision"
