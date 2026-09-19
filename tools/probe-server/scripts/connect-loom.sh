#!/usr/bin/env bash
# Optional helper: start the probe server, then print how to join from Loom.
# A headless Xvfb session in this environment failed GLFW (no GLX). Use a machine
# with a working OpenGL context. 1.20.1 Loom ignores --server/--port; use Quick Play
# on versions that support it, or click the server in the multiplayer list.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$ROOT/../.." && pwd)"
VERSION="${1:-1.20.1}"
CONFIG="${2:-default}"
PORT="${PROBE_PORT:-25565}"
echo "1) Start:  $ROOT/scripts/run-${VERSION}.sh $CONFIG"
echo "2) Wait for READY"
echo "3) Launch the matching Fabric client with NoFingerprint."
echo "   Loom (from $REPO):"
echo "     JAVA_TOOL_OPTIONS=-Dnofingerprint.debug.probes=true ./gradlew :${VERSION}:runClient"
echo "   Then join 127.0.0.1:${PORT} from the multiplayer screen."
echo "   Do not use --server on 1.20.1; Fabric Loader logs 'Completely ignored arguments'."
echo "4) Offline username is enough. For cache isolation, join twice with different names."
echo "5) The probe server writes runtime-probe-results.json at the repo root when the session ends (--once) or on Ctrl+C."
