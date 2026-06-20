#!/usr/bin/env bash
# Valida i modelli del server faro: JSON ben formati, namespacing sotto custom/,
# riferimenti esistenti e -- soprattutto -- nessun custom_model_data duplicato
# sullo stesso base item (il "conflitto di merge" dei modelli). Sola lettura.
#
# Uso: scripts/validate-models.sh
# Richiede: python3 con PyYAML.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

exec python3 - "$ROOT_DIR" <<'PY'
import sys, os, json, glob

root = sys.argv[1]
try:
    import yaml
except ImportError:
    print("ERRORE: serve PyYAML (pip install pyyaml).", file=sys.stderr)
    sys.exit(2)

errors = []
def err(msg): errors.append(msg)

# --- registro id ---
reg_path = os.path.join(root, "community-server", "registry", "model-ids.yml")
allocated = 0
if os.path.isfile(reg_path):
    with open(reg_path) as f:
        reg = yaml.safe_load(f) or {}
    base_items = reg.get("base_items") or {}
    for item, entries in base_items.items():
        seen = {}
        for e in (entries or []):
            cmd = e.get("cmd")
            if cmd is None:
                err(f"model-ids.yml: voce senza 'cmd' su {item}."); continue
            allocated += 1
            if cmd in seen:
                err(f"model-ids.yml: custom_model_data {cmd} DUPLICATO su {item} "
                    f"(usato da '{seen[cmd]}' e '{e.get('owner')}').")
            seen[cmd] = e.get("owner")

# --- JSON dei modelli del server faro ---
pack_root = os.path.join(root, "community-server", "assets", "resource-pack")
models_glob = os.path.join(pack_root, "assets", "*", "models", "custom", "**", "*.json")
checked = 0
for jpath in glob.glob(models_glob, recursive=True):
    checked += 1
    rel = os.path.relpath(jpath, root)
    try:
        with open(jpath) as f:
            model = json.load(f)
    except Exception as e:
        err(f"{rel}: JSON non valido: {e}"); continue
    if "/custom/" not in jpath.replace("\\", "/"):
        err(f"{rel}: i modelli devono stare sotto models/custom/.")
    # i textures/parent referenziati devono esistere nel pack
    refs = []
    parent = model.get("parent")
    if isinstance(parent, str):
        refs.append(("model", parent))
    for v in (model.get("textures") or {}).values():
        if isinstance(v, str) and not v.startswith("#"):
            refs.append(("texture", v))
    for kind, ref in refs:
        if ":" in ref:
            ns, path = ref.split(":", 1)
        else:
            ns, path = "minecraft", ref
        if ns == "minecraft" and path.startswith(("block/", "item/", "builtin/")):
            continue  # riferimento vanilla, non in repo
        sub = "models" if kind == "model" else "textures"
        ext = ".json" if kind == "model" else ".png"
        target = os.path.join(pack_root, "assets", ns, sub, path + ext)
        if not os.path.isfile(target):
            err(f"{rel}: {kind} '{ref}' non trovato ({os.path.relpath(target, root)}).")

if errors:
    print("Validazione modelli FALLITA:\n", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)
print(f"OK: {checked} modelli, {allocated} id allocati senza collisioni.")
PY
