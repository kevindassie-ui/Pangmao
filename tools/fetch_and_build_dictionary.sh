#!/usr/bin/env bash
set -euo pipefail

work_dir="$(mktemp -d)"
cc_dir="$work_dir/cc-cedict"
tatoeba_dir="$work_dir/tatoeba"
unihan_dir="$work_dir/unihan"
mkdir -p "$unihan_dir/files"

git clone --quiet https://github.com/edvardsr/cc-cedict.git "$cc_dir"
git -C "$cc_dir" checkout --quiet 3e29e175d6186f76a6978d8716d0976a2016923f

git clone --quiet https://github.com/leonsilicon/tatoeba-sentence-pairs-in-mandarin-chinese-english.git "$tatoeba_dir"
git -C "$tatoeba_dir" checkout --quiet 7df8b93466f009310de22273973b34de7a20bf62

curl --fail --location --retry 4 --silent --show-error \
  --output "$work_dir/cfdict.u8" \
  https://chine.in/assets/cfdict/cfdict.u8
curl --fail --location --retry 4 --silent --show-error \
  --output "$unihan_dir/Unihan.zip" \
  https://www.unicode.org/Public/17.0.0/ucd/Unihan.zip

printf '%s  %s\n' \
  '0cb61b6e65c41a07fd374d51594a9f7c01c2fc0573fad7227b74aecdfd214931' "$cc_dir/data/all.js" \
  '430a2c57b78a7ae57e28d5d70c4ce9c4fca1a1680279d8685a89b4caa6f3bf7c' "$tatoeba_dir/data/Sentence pairs in Mandarin Chinese-English - 2026-05-20.tsv" \
  '124d87f0fc2aed305e42ec3794584fa62bf00df9ed0aac45b950aba518795e75' "$work_dir/cfdict.u8" \
  'f7a48b2b545acfaa77b2d607ae28747404ce02baefee16396c5d2d7a8ef34b5e' "$unihan_dir/Unihan.zip" \
  | sha256sum --check --strict

unzip -q "$unihan_dir/Unihan.zip" -d "$unihan_dir/files"

python3 tools/build_dictionary.py \
  --cc-cedict "$cc_dir/data/all.js" \
  --cfdict "$work_dir/cfdict.u8" \
  --tatoeba "$tatoeba_dir/data/Sentence pairs in Mandarin Chinese-English - 2026-05-20.tsv" \
  --pangmao-examples tools/data/pangmao_examples.tsv \
  --reviewed-definitions tools/data/reviewed_definitions.tsv \
  --unihan-dir "$unihan_dir/files" \
  --output app/src/main/assets/databases/pangmao.db \
  --cc-revision 3e29e175d6186f76a6978d8716d0976a2016923f \
  --tatoeba-release 2026-05-20
