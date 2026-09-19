# Runtime Probe Results

NoFingerprint **1.1.7.1-nofingerprint.2**.  
Harness: `tools/probe-server` (`minecraft-protocol` **1.68.0**, BSD-3-Clause, plus `minecraft-data`).  
Date: **2026-09-19**.

This document distinguishes **RUNTIME VERIFIED** (a real process produced the observation) from **CODE VERIFIED ONLY** (Phase 1 unit tests / source) and **NOT TESTED**.

Machine-readable companion: `runtime-probe-results.json`.

## Environment

| | |
|---|---|
| OS | Linux 7.2.4 |
| Host Java | OpenJDK 25.0.4.1 (Gradle / Loom). Temurin 17.0.19 available but cannot run Stonecutter (needs JVM 21+). |
| Fabric Loader | 0.19.3 (Loom `runClient`) |
| Fabric API | 0.92.6+1.20.1 (1.20.1 run) |
| Minecraft | 1.20.1 (Loom client started; no play join). 1.21.11 (protocol self-test only). 26.2 (library gap). |
| NoFingerprint | 1.1.7.1-nofingerprint.2 |
| Probe bind | 127.0.0.1 only, `online-mode: false` |
| Display | Xvfb :99 without GLX. GLFW error 65542: `GLX: Failed to load GLX`. |

## Brand

| Config | Tag | Observation |
|---|---|---|
| Protocol bot 1.20.1 / 1.21.11 | **INCONCLUSIVE** | `minecraft-protocol` offline bot sent no `minecraft:brand`. Silence is not PASS. |
| NF default, 1.20.1 Loom | **CODE VERIFIED ONLY** for the wire; **RUNTIME VERIFIED** for the decision | Client log: `spoofAsVanilla=false brand=false`. Expected on-wire brand is still Fabric. Packet not captured (no join). |
| NF strict | **NOT TESTED** | Needs `spoofAsVanilla=true` and a play join. |
| NF + EP | **NOT TESTED** | ExploitPreventer was not installed. |
| Fabric without NF | **NOT TESTED** | Loom `runClient` always loads this project. |

Expected (code): default → `fabric`; strict → `vanilla`. Phase 1 keeps brand spoof available under EP.

## Channels

| Config | Tag | Observation |
|---|---|---|
| Protocol bot | **INCONCLUSIVE** | No `minecraft:register` list (typical for a non-Fabric bot). |
| NF default init | **RUNTIME VERIFIED** (policy only) | `channels=true` with default AUTO whitelist. |
| On-wire Fabric/mod channels | **NOT TESTED** | No play session. |
| `nofingerprint` as a channel | **NOT TESTED** on the wire | Source does not register a NoFingerprint plugin channel; still needs a join to prove. |

## Inbound payload behavior

| Version | Tag | Observation |
|---|---|---|
| 1.20.1 | **NOT_SUPPORTED** | No typed Fabric payload codecs. Unknown custom payloads are a vanilla ignore path. |
| 1.21.11 protocol bot | **INCONCLUSIVE** | Bot is not Fabric. Staying connected after unknown custom payloads is **not** NF codec protection and is not scored PASS. |
| NF 1.21.11 client vs Fabric baseline | **NOT TESTED** | |

Phase 1 mixin `PayloadTypeRegistryImplMixin` remains **CODE VERIFIED ONLY**.

## Known packs

| Version | Tag | Observation |
|---|---|---|
| 1.20.1 | **NOT_SUPPORTED** | Protocol has no `select_known_packs`. |
| 1.21.11 bot | **INCONCLUSIVE** | Empty / missing useful response; bot is not Fabric and has no mod packs. |
| NF 1.21.11 + Fabric hook | **CODE VERIFIED ONLY** | Mixin filters when `ModRegistry.isKnownPacksHookPresent()` (Fabric 1.21.11+). Not seen on the wire. |

## Translation probing

**INCONCLUSIVE / NOT TESTED.** Sign-editor echo requires a player to close the editor. The harness sends `open_sign_editor` and a translatable system chat with `nofingerprint_test.fake_mod_key`. No `update_sign` arrived from the protocol bot.

Client init: `translation=true` on NF default — **RUNTIME VERIFIED** as a feature flag only.

## Keybind probing

**INCONCLUSIVE / NOT TESTED.** Same sign-echo path (`key.jump` / `key.fake_mod.open_gui`). Default `fakeDefaultKeybinds=true` is **CODE VERIFIED ONLY**.

## Resource packs

### Local URL (`http://127.0.0.1:<port>/test.zip`)

| | Tag | Observation |
|---|---|---|
| 1.20.1 NF vs GET | **NOT TESTED** | No play join, so HttpUtil mixin never ran. |
| Harness HTTP | **RUNTIME VERIFIED** (listener) | Pack server binds `127.0.0.1`, records TCP and HTTP. During the failed Loom launch it saw `GET /` and `GET /json/version` only — **not** `GET /test.zip`. Those hits are not a pack download. |
| Phase 1 unit tests | **CODE VERIFIED** | `PrivateAddressClassifierTest`, `RedirectPolicyTest` (local HttpServer 302/308). |

### Redirect 308

**INCONCLUSIVE** for a live client. The Minecraft pack URL used for 308 in this harness is itself loopback, so a correct NF block never follows the hop. A public-classified bindable origin was not available without adding extra addresses. Do not treat that as PASS.

### Pack status packets

**NOT TESTED** (no client resource-pack status).

## Cache isolation

**NOT TESTED.** Needs two play joins with different account UUIDs (offline names suffice) and GET counts / `downloads/<uuid>/` paths.

Default decision `isolateCache=true` is **RUNTIME VERIFIED** at client init.

## Chat signing

**INCONCLUSIVE.** Offline mode; no Mojang profile key. Default `unsignedChat=false` (SIGN) **RUNTIME VERIFIED** at client init. ON vs OFF packet signatures **NOT TESTED**.

## Timing observations

Protocol bot 1.20.1: TCP/login/playerJoin clustered at ~500ms from server start; kick `nf-probe-complete` ~4s later. **RUNTIME VERIFIED** for the harness, not for a real client.

1.20.1 Loom: `--server` / `--port` **ignored** (`Completely ignored arguments`). Join must be from the multiplayer list or a Quick Play flag on versions that honor it.

No timing obfuscation was added.

## ExploitPreventer coexistence

| | Tag |
|---|---|
| EP loaded | **NOT TESTED** (mod not present) |
| EP absent, default config dump | **RUNTIME VERIFIED** |

Dump (`-Dnofingerprint.debug.probes=true`, 1.20.1 Loom):

```
mc=1.20.1 ep=false ncr=false npe=false spoofAsVanilla=false
brand=false channels=true knownPacks=true isolateCache=true
blockLocalUrls=true translation=true stripShaders=true
unsignedChat=false telemetry=true
```

`brand=false` here means “vanilla spoof is off”, not “channels are off”. Channels/known-packs/cache/URL/translation stay enabled. That matches Phase 1 when EP is absent. Dual-install was not run.

## Unverified items

- Wire brand, register list, and “no `nofingerprint` channel” on a real Fabric client
- Inbound Fabric codec fingerprint (DecoderException vs ignore) vs baseline Fabric
- Known-pack filtering on 1.21.11 Fabric
- Translation/keybind echo
- 1.20.1 localhost pack GET suppression (the original Phase 2 question)
- 308 from a public-classified URL
- Cross-account cache GET counts
- Chat session / signed chat
- NF + ExploitPreventer
- Minecraft **26.2** sessions (`minecraft-protocol` 1.68.0 lists **26.1**, not 26.2) — **NOT_SUPPORTED**
- AUTO whitelist leaking Simple Voice Chat / Xaero

## How to finish the live matrix

1. `tools/probe-server/scripts/run-1.20.1.sh default` and `run-1.21.11.sh default` (then `strict`).
2. Join `127.0.0.1` from a GPU-capable Fabric client. Do not pass `--server` on 1.20.1 Loom.
3. `JAVA_TOOL_OPTIONS=-Dnofingerprint.debug.probes=true` on the client.
4. Ctrl+C or `--once` writes `runtime-probe-results.json`.
5. Repeat with ExploitPreventer in `mods/` for config `ep`.

See `tools/probe-server/scripts/connect-loom.sh`.
