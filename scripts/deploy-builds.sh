#!/usr/bin/env bash
# Incolla in produzione le costruzioni approvate e non ancora deployate.
# Legge community-server/registry/claims.yml e, per ogni claim con
# status=approved e deployed=false, manda al server prod via RCON il comando
# `orp-build import <slug>`.
#
# Uso:
#   scripts/deploy-builds.sh --dry-run     # elenca soltanto, nessuna connessione
#   scripts/deploy-builds.sh               # deploy reale (richiede i secret RCON)
#
# Variabili d'ambiente per il deploy reale:
#   PROD_RCON_HOST  PROD_RCON_PORT  PROD_RCON_PASSWORD
#
# Dopo un import riuscito segna `deployed: true` nel registro (commit a parte).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY_RUN="false"
[ "${1:-}" = "--dry-run" ] && DRY_RUN="true"

exec python3 - "$ROOT_DIR" "$DRY_RUN" <<'PY'
import sys, os, struct, socket

root, dry_run = sys.argv[1], sys.argv[2] == "true"
try:
    import yaml
except ImportError:
    print("ERRORE: serve PyYAML (pip install pyyaml).", file=sys.stderr)
    sys.exit(2)

claims_path = os.path.join(root, "community-server", "registry", "claims.yml")
server_path = os.path.join(root, "community-server", "server.yml")

with open(claims_path) as f:
    data = yaml.safe_load(f) or {}
claims = data.get("claims") or []

import_tmpl = "orp-build import {slug}"
if os.path.isfile(server_path):
    with open(server_path) as f:
        scfg = yaml.safe_load(f) or {}
    import_tmpl = (scfg.get("deploy") or {}).get("import_command", import_tmpl)

pending = [c for c in claims if c.get("status") == "approved" and not c.get("deployed")]
if not pending:
    print("Nessuna costruzione approvata da incollare. Niente da fare.")
    sys.exit(0)

print(f"Da incollare: {len(pending)} costruzioni")
for c in pending:
    print(f"  - {c['id']}  ->  /{import_tmpl.format(slug=c['id'])}")

if dry_run:
    print("\n[dry-run] nessuna connessione effettuata.")
    sys.exit(0)

# --- client RCON (Source protocol) minimale, nessuna dipendenza esterna ---
host = os.environ.get("PROD_RCON_HOST")
port = os.environ.get("PROD_RCON_PORT")
password = os.environ.get("PROD_RCON_PASSWORD")
if not (host and port and password):
    print("ERRORE: per il deploy reale servono PROD_RCON_HOST/PORT/PASSWORD.", file=sys.stderr)
    sys.exit(2)

SERVERDATA_AUTH, SERVERDATA_EXECCOMMAND = 3, 2

def rcon_packet(req_id, ptype, body):
    payload = struct.pack("<ii", req_id, ptype) + body.encode("utf8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload

def rcon_read(sock):
    raw_len = sock.recv(4)
    if len(raw_len) < 4:
        raise IOError("risposta RCON troncata")
    (length,) = struct.unpack("<i", raw_len)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise IOError("connessione RCON chiusa")
        data += chunk
    req_id, ptype = struct.unpack("<ii", data[:8])
    return req_id, ptype, data[8:-2].decode("utf8", "replace")

with socket.create_connection((host, int(port)), timeout=10) as sock:
    sock.sendall(rcon_packet(1, SERVERDATA_AUTH, password))
    rid, _, _ = rcon_read(sock)
    if rid == -1:
        print("ERRORE: autenticazione RCON fallita.", file=sys.stderr)
        sys.exit(2)
    for c in pending:
        cmd = import_tmpl.format(slug=c["id"])
        sock.sendall(rcon_packet(2, SERVERDATA_EXECCOMMAND, cmd))
        _, _, resp = rcon_read(sock)
        print(f"  -> /{cmd}: {resp.strip() or 'ok'}")
        c["deployed"] = True

# riscrive il registro con deployed=true (un workflow committa la modifica)
with open(claims_path, "w") as f:
    yaml.safe_dump(data, f, sort_keys=False, allow_unicode=True)
print("\nFatto. Aggiornato deployed=true nel registro (committa la modifica).")
PY
