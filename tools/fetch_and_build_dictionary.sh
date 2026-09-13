#!/usr/bin/env bash
set -euo pipefail

work_dir="$(mktemp -d)"
cc_dir="$work_dir/cc-cedict"
tatoeba_dir="$work_dir/tatoeba"
unihan_dir="$work_dir/unihan"
freedict_fr_dir="$work_dir/freedict-fra-zho"
freedict_en_dir="$work_dir/freedict-eng-zho"
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
curl --fail --location --retry 4 --silent --show-error \
  --output "$work_dir/freedict-fra-zho.tar.xz" \
  https://download.freedict.org/dictionaries/fra-zho/2025.11.23/freedict-fra-zho-2025.11.23.src.tar.xz
curl --fail --location --retry 4 --silent --show-error \
  --output "$work_dir/freedict-eng-zho.tar.xz" \
  https://download.freedict.org/dictionaries/eng-zho/2025.11.23/freedict-eng-zho-2025.11.23.src.tar.xz

printf '%s  %s\n' \
  '0cb61b6e65c41a07fd374d51594a9f7c01c2fc0573fad7227b74aecdfd214931' "$cc_dir/data/all.js" \
  '430a2c57b78a7ae57e28d5d70c4ce9c4fca1a1680279d8685a89b4caa6f3bf7c' "$tatoeba_dir/data/Sentence pairs in Mandarin Chinese-English - 2026-05-20.tsv" \
  '124d87f0fc2aed305e42ec3794584fa62bf00df9ed0aac45b950aba518795e75' "$work_dir/cfdict.u8" \
  'f7a48b2b545acfaa77b2d607ae28747404ce02baefee16396c5d2d7a8ef34b5e' "$unihan_dir/Unihan.zip" \
  | sha256sum --check --strict

printf '%s  %s\n' \
  '3a101af1bdd523e29f217dd8612ade44dbc73b65b4ea1358ac778ebe155a2cd8b02e3816cb1a9f1dd6500937fa35825d1544db57c5a8aedf71a365c57cdf188a' "$work_dir/freedict-fra-zho.tar.xz" \
  '25aed0f1d7de68919aa9da1ba92d67f566ae4ea81660f42071c81fc21e56d4b210d61df379315678648c45ca7e52c4a0ba2eec009fbaab7c72e7472489e1fc4c' "$work_dir/freedict-eng-zho.tar.xz" \
  | sha512sum --check --strict

unzip -q "$unihan_dir/Unihan.zip" -d "$unihan_dir/files"
mkdir -p "$freedict_fr_dir" "$freedict_en_dir"
tar --no-same-owner -xJf "$work_dir/freedict-fra-zho.tar.xz" -C "$freedict_fr_dir"
tar --no-same-owner -xJf "$work_dir/freedict-eng-zho.tar.xz" -C "$freedict_en_dir"
freedict_fr_tei="$(find "$freedict_fr_dir" -type f -name 'fra-zho.tei' -print -quit)"
freedict_en_tei="$(find "$freedict_en_dir" -type f -name 'eng-zho.tei' -print -quit)"
test -n "$freedict_fr_tei"
test -n "$freedict_en_tei"

python3 tools/build_dictionary.py \
  --cc-cedict "$cc_dir/data/all.js" \
  --cfdict "$work_dir/cfdict.u8" \
  --tatoeba "$tatoeba_dir/data/Sentence pairs in Mandarin Chinese-English - 2026-05-20.tsv" \
  --tatoeba-french tools/data/tatoeba_french_examples.tsv \
  --pangmao-examples tools/data/pangmao_examples.tsv \
  --reviewed-definitions tools/data/reviewed_definitions.tsv \
  --unihan-dir "$unihan_dir/files" \
  --freedict-french "$freedict_fr_tei" \
  --freedict-english "$freedict_en_tei" \
  --output app/src/main/assets/databases/pangmao.db \
  --cc-revision 3e29e175d6186f76a6978d8716d0976a2016923f \
  --tatoeba-release 2026-05-20 \
  --freedict-release 2025.11.23
