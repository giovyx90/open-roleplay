#!/usr/bin/env bash
# Valida le costruzioni del server faro: manifest, schematic e -- soprattutto --
# le sovrapposizioni tra aree (il "conflitto di merge" spaziale di un mondo non
# piatto). Sola lettura. Esce con codice != 0 se qualcosa non va.
#
# Uso: scripts/validate-builds.sh
# Richiede: python3 con PyYAML (pip install pyyaml).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

exec python3 - "$ROOT_DIR" <<'PY'
import sys, os, gzip, glob

root = sys.argv[1]
try:
    import yaml
except ImportError:
    print("ERRORE: serve PyYAML (pip install pyyaml).", file=sys.stderr)
    sys.exit(2)

errors = []
def err(msg): errors.append(msg)

builds_dir = os.path.join(root, "community-server", "builds")
claims_path = os.path.join(root, "community-server", "registry", "claims.yml")
server_path = os.path.join(root, "community-server", "server.yml")

# --- carica i confini del mondo (opzionali) ---
world_bounds = {}
if os.path.isfile(server_path):
    with open(server_path) as f:
        server = yaml.safe_load(f) or {}
    for wname, wcfg in (server.get("worlds") or {}).items():
        b = (wcfg or {}).get("bounds")
        if b:
            world_bounds[wname] = b

REQUIRED = ["id", "world", "anchor", "size", "rotation", "license", "attribution"]

def vec(d, keys=("x", "y", "z")):
    return tuple(int(d[k]) for k in keys)

def load_manifest(path):
    with open(path) as f:
        return yaml.safe_load(f) or {}

def aabb_from_manifest(m):
    """Calcola (min, max) dal manifest. anchor = angolo minimo; rotation 90/270
    scambia le estensioni su x e z."""
    ax, ay, az = vec(m["anchor"])
    sx, sy, sz = vec(m["size"])
    rot = int(m["rotation"]) % 360
    if rot in (90, 270):
        sx, sz = sz, sx
    return (ax, ay, az), (ax + sx, ay + sy, az + sz)

def overlaps(a_min, a_max, b_min, b_max):
    """True se i due volumi condividono interno (le facce a contatto NON contano)."""
    for i in range(3):
        if a_max[i] <= b_min[i] or b_max[i] <= a_min[i]:
            return False
    return True

# --- carica i claim ---
claims = []
if os.path.isfile(claims_path):
    with open(claims_path) as f:
        data = yaml.safe_load(f) or {}
    claims = data.get("claims") or []
claims_by_id = {}
for c in claims:
    cid = c.get("id")
    if not cid:
        err("claims.yml: un claim non ha 'id'."); continue
    if cid in claims_by_id:
        err(f"claims.yml: claim '{cid}' duplicato.")
    claims_by_id[cid] = c

# --- scorri le build ---
build_ids = set()
for manifest_path in sorted(glob.glob(os.path.join(builds_dir, "*", "build.yml"))):
    slug = os.path.basename(os.path.dirname(manifest_path))
    placeholder = slug.startswith("_")
    try:
        m = load_manifest(manifest_path)
    except Exception as e:
        err(f"{slug}: build.yml non parsabile: {e}"); continue

    for k in REQUIRED:
        if k not in m:
            err(f"{slug}: manca il campo obbligatorio '{k}' in build.yml.")
    if any(k not in m for k in REQUIRED):
        continue

    if m["id"] != slug:
        err(f"{slug}: id '{m['id']}' diverso dal nome cartella '{slug}'.")
    if int(m["rotation"]) % 360 not in (0, 90, 180, 270):
        err(f"{slug}: rotation deve essere 0/90/180/270.")
    build_ids.add(slug)

    # schematic obbligatorio solo per build reali
    schem = os.path.join(os.path.dirname(manifest_path), "region.schem")
    if not placeholder:
        if not os.path.isfile(schem):
            err(f"{slug}: manca region.schem.")
        else:
            with open(schem, "rb") as fh:
                magic = fh.read(2)
            if magic != b"\x1f\x8b":
                err(f"{slug}: region.schem non sembra uno schematic valido (atteso gzip/NBT).")

    a_min, a_max = aabb_from_manifest(m)

    # confini del mondo
    b = world_bounds.get(m["world"])
    if b:
        bmin, bmax = vec(b["min"]), vec(b["max"])
        for i, ax in enumerate("xyz"):
            if a_min[i] < bmin[i] or a_max[i] > bmax[i]:
                err(f"{slug}: esce dai confini del mondo '{m['world']}' su asse {ax}.")

    # claim corrispondente
    if placeholder:
        if slug in claims_by_id:
            err(f"{slug}: i template (cartelle '_*') non devono avere un claim.")
        continue
    c = claims_by_id.get(slug)
    if not c:
        err(f"{slug}: manca il claim in registry/claims.yml.")
        continue
    if c.get("world") != m["world"]:
        err(f"{slug}: il claim ha world '{c.get('world')}' != manifest '{m['world']}'.")
    c_min, c_max = vec(c["min"]), vec(c["max"])
    if c_min != a_min or c_max != a_max:
        err(f"{slug}: il claim {c_min}-{c_max} non combacia con l'AABB del manifest {a_min}-{a_max}.")

# claim orfani (senza cartella build)
for cid in claims_by_id:
    if cid not in build_ids:
        err(f"claims.yml: il claim '{cid}' non ha una cartella in builds/{cid}/.")

# --- il controllo chiave: sovrapposizioni tra claim ---
valid = []
for c in claims:
    try:
        valid.append((c["id"], c.get("world"), vec(c["min"]), vec(c["max"])))
    except Exception:
        pass
for i in range(len(valid)):
    for j in range(i + 1, len(valid)):
        id_a, w_a, min_a, max_a = valid[i]
        id_b, w_b, min_b, max_b = valid[j]
        if w_a == w_b and overlaps(min_a, max_a, min_b, max_b):
            err(f"SOVRAPPOSIZIONE: '{id_a}' e '{id_b}' occupano lo stesso volume in '{w_a}'. "
                f"Sposta l'anchor o coordina con l'altro autore (conflitto di merge spaziale).")

if errors:
    print("Validazione costruzioni FALLITA:\n", file=sys.stderr)
    for e in errors:
        print(f"  - {e}", file=sys.stderr)
    sys.exit(1)
print(f"OK: {len(build_ids)} costruzioni valide, {len(valid)} claim senza sovrapposizioni.")
PY
