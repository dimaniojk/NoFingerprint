# NoFingerprint probe server

Local, offline Minecraft protocol harness for developers. It is **not** packaged into the NoFingerprint mod JAR and is **not** part of the end-user download.

## Library

| | |
|---|---|
| Package | [`minecraft-protocol`](https://github.com/PrismarineJS/node-minecraft-protocol) 1.68.0 |
| Data | `minecraft-data` (protocol definitions) |
| License | BSD-3-Clause |
| Why | Packet-level client/server without Paper/Spigot; supports 1.20.1 and 1.21.11 |

`mc.supportedVersions` is printed at startup. **26.2** is accepted as an architecture flag; if the library does not list `26.2`, the process exits with `NOT_SUPPORTED` rather than faking a session.

## Safety

- `online-mode: false`
- Binds default `127.0.0.1` only
- Resource-pack HTTP listens on `127.0.0.1`
- Does not scan LAN or public hosts

## Run

```bash
# from repo root, Node 22+
cd tools/probe-server
npm install
./scripts/run-1.20.1.sh default
./scripts/run-1.21.11.sh strict
```

The process prints `READY` then waits for a Minecraft client.

Connect a Fabric client (with or without NoFingerprint) to the printed host/port. Offline username is enough.

Requested client configs (you apply these in the client; the server records whatever it actually receives):

| `--config` | Meaning |
|---|---|
| `baseline` | Fabric, no NoFingerprint |
| `default` | NoFingerprint, `spoofAsVanilla=false` |
| `strict` | NoFingerprint, `spoofAsVanilla=true` |
| `ep` | NoFingerprint + ExploitPreventer |
| `known-mod` | Optional extra networking mod |
| `self-test` | Internal vanilla-protocol bot (not a Fabric client) |

## Outputs

- `tools/probe-server/results/<run>.jsonl` — one JSON object per probe
- `runtime-probe-results.json` and `RUNTIME_PROBE_RESULTS.md` at the repo root after `--write-report` (default on `--once`)

## Minecraft auto-connect (optional)

Vanilla/Fabric accept `--quickPlayMultiplayer 127.0.0.1:<port>` on 1.20.5+.

**1.20.1 Loom does not honor `--server` / `--port`.** Fabric Loader logs `Completely ignored arguments`. Join from the multiplayer list, or see `scripts/connect-loom.sh`.

Headless Xvfb without GLX cannot start GLFW (`error 65542`). Use a GPU-capable display.

Use `-Dnofingerprint.debug.probes=true` (or `NOFINGERPRINT_DEBUG_PROBES=true`) on the client JVM for feature-decision logs. Tokens are never logged.
