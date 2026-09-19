#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
CONFIG="${1:-default}"
PORT="${PROBE_PORT:-25567}"
echo "26.2: minecraft-protocol 1.68.0 may not list this version. The process records NOT_SUPPORTED instead of faking a session."
exec nix-shell -p nodejs_24 --run "node src/index.js --version 26.2 --config '$CONFIG' --host 127.0.0.1 --port $PORT --nf-version 1.1.7.1-nofingerprint.2-beta.1"
