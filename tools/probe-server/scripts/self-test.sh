#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
VERSION="${1:-1.20.1}"
PORT="${PROBE_PORT:-25570}"
exec nix-shell -p nodejs_24 --run "node src/index.js --version '$VERSION' --self-test --once --port $PORT --timeout-ms 30000 --settle-ms 2500 --nf-version 1.1.7.1-nofingerprint.2-beta.1"
