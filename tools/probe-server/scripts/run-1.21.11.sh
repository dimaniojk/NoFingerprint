#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
CONFIG="${1:-default}"
PORT="${PROBE_PORT:-25566}"
exec nix-shell -p nodejs_24 --run "node src/index.js --version 1.21.11 --config '$CONFIG' --host 127.0.0.1 --port $PORT --nf-version 1.1.7.1-nofingerprint.2-beta.1"
