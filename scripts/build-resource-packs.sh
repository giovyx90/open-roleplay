#!/usr/bin/env bash
set -eo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${1:-"$ROOT_DIR/target/resource-packs"}"

mkdir -p "$OUTPUT_DIR"

found_pack=false

for pack_dir in "$ROOT_DIR"/*/assets/resource-pack; do
  [ -d "$pack_dir" ] || continue
  found_pack=true

  module_dir="$(dirname "$(dirname "$pack_dir")")"
  module_name="$(basename "$module_dir")"

  if [ ! -f "$pack_dir/pack.mcmeta" ]; then
    echo "Errore: $module_name non contiene pack.mcmeta" >&2
    exit 1
  fi

  if [ ! -d "$pack_dir/assets" ]; then
    echo "Errore: $module_name non contiene assets/" >&2
    exit 1
  fi

  zip_path="$OUTPUT_DIR/$module_name-resource-pack.zip"
  tmp_zip="$zip_path.tmp"
  rm -f "$tmp_zip" "$zip_path"

  (
    cd "$pack_dir"
    zip_entries=(pack.mcmeta assets)
    if [ -f pack.png ]; then
      zip_entries+=(pack.png)
    fi

    zip -qr "$tmp_zip" "${zip_entries[@]}" \
      -x "*/.DS_Store" \
      -x "__MACOSX/*"
  )

  mv "$tmp_zip" "$zip_path"
  echo "Creato: $zip_path"
done

if [ "$found_pack" = false ]; then
  echo "Errore: nessun resource pack trovato in */assets/resource-pack" >&2
  exit 1
fi
