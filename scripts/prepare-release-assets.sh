#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${1:-"$ROOT_DIR/release-assets"}"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

copy_module_jar() {
  local module="$1"
  local asset_name="$2"
  local jar

  jar="$(find "$ROOT_DIR/$module/target" -maxdepth 1 -type f \
    -name "$module-*.jar" \
    ! -name "original-*.jar" \
    ! -name "*-sources.jar" \
    ! -name "*-javadoc.jar" \
    | sort | tail -n 1)"

  if [ -z "$jar" ]; then
    echo "Errore: JAR non trovato per $module" >&2
    exit 1
  fi

  cp "$jar" "$OUT_DIR/$asset_name"
  echo "Aggiunto: $asset_name"
}

copy_module_jar "open-core-api" "open-core-api.jar"
copy_module_jar "open-core-paper" "open-core-paper.jar"
copy_module_jar "open-access" "open-access.jar"
copy_module_jar "open-cosmetics" "open-cosmetics.jar"
copy_module_jar "open-weapons" "open-weapons.jar"
copy_module_jar "open-vending-machines" "open-vending-machines.jar"
copy_module_jar "open-companies" "open-companies.jar"

if compgen -G "$ROOT_DIR/target/resource-packs/*.zip" > /dev/null; then
  cp "$ROOT_DIR"/target/resource-packs/*.zip "$OUT_DIR"/
fi

(
  cd "$OUT_DIR"
  sha256sum * > SHA256SUMS.txt
)

echo "Asset release pronti in: $OUT_DIR"
