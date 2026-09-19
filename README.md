# NoFingerprint

NoFingerprint is a client-side Fabric privacy mod focused on reducing unnecessary client fingerprinting and information disclosure to Minecraft servers.

This is an independently maintained [GPL-3.0](LICENSE) fork of [OpSec](https://github.com/aurickk/OpSec) by aurickk. The original author does not endorse this fork.

This is a **public beta**. It is **not** guaranteed to be undetectable. It is **not** an anti-cheat bypass. It is **not** a guarantee of complete anonymity.

> [!IMPORTANT]
> Only download NoFingerprint from official NoFingerprint sources. Third-party reuploads, Discord attachments, and random file hosts are not official and may be malicious.
>
> Intended official source:
> - **[GitHub Releases](https://github.com/dimaniojk/NoFingerprint/releases)**
>
> Upstream OpSec pages (Modrinth, CurseForge, and aurickk/OpSec releases) distribute **OpSec**, not NoFingerprint.

## Features

Implemented today (see [Important limitations](#important-limitations) and [RUNTIME_PROBE_RESULTS.md](RUNTIME_PROBE_RESULTS.md) for what is live-tested vs code-only):

- Configurable client brand spoofing (`spoofAsVanilla`, **off** by default)
- Channel / custom payload filtering, with AUTO or CUSTOM mod whitelist
- Known-pack filtering where Fabric exposes the known-packs hook (Minecraft 1.21.11+ with modern Fabric API)
- Translation and keybind probe mitigation
- Per-account resource-pack cache isolation
- Local / private resource-pack URL blocking, including CGNAT (`100.64.0.0/10`) and IPv6 unique-local (`fc00::/7`)
- Redirect validation on HTTP 300–303, 305, 307, and 308 (every hop)
- Shader override filtering for non-whitelisted mods
- Telemetry blocking
- Chat signing controls (OFF / AUTO / ON)
- Compatibility handling for ExploitPreventer, No Chat Reports, and No Prying Eyes
- Optional account manager (session / offline accounts)
- Optional bypass of required server resource packs (MANUAL / ASK / ALWAYS)

## Important limitations

- **`spoofAsVanilla` is OFF by default.** With the default Fabric brand, a server can still see that the client is Fabric.
- **Allowed mod channels may reveal mods.** AUTO whitelist keeps networking mods working and can expose their channels.
- **DNS rebinding is not fully prevented.** Address classification and `HttpURLConnection` can resolve a hostname separately (PARTIALLY MITIGATED).
- **Behavioral fingerprinting may still exist** (timing, pack status, login plugin replies, and other protocol side channels).
- **Some protections are version-specific** (known-pack filtering; typed payload codec handling from 1.20.5+).
- **Minecraft 26.2 runtime probing is currently unavailable** (`minecraft-protocol` 1.68.0 lists 26.1, not 26.2).
- **Runtime verification is incomplete.** Most protections are **CODE VERIFIED**. A live Fabric join against the probe harness has not been completed. See [RUNTIME_PROBE_RESULTS.md](RUNTIME_PROBE_RESULTS.md).
- **Account Manager stores access and refresh tokens in plaintext** in `config/nofingerprint-accounts.json`.

## Compatibility

- **ExploitPreventer:** can be installed together. NoFingerprint stands down only on overlapping HTTP download and translation/keybind wrap paths. Brand spoof, channel filtering, known packs, cache isolation, pack bypass, shaders, chat signing, telemetry, and the account manager stay available.
- **No Chat Reports / No Prying Eyes:** chat-signing and related UI are deferred to those mods when present.
- Network-heavy mods (Simple Voice Chat, map mods, and similar) can still expose their presence through allowed channels when whitelist mode is AUTO or CUSTOM.

## Supported versions

Stonecutter matrix: **1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.6, 1.21.9, 1.21.11, 26.1, 26.2**.

This beta ships representative artifacts for **1.20.1**, **1.21.11**, and **26.2**. Other matrix versions can be built from source.

| Target | Java | This beta JAR | Runtime vs code |
|--------|------|---------------|-----------------|
| 1.20.1 (covers 1.20–1.20.1) | 17 | shipped | **BUILD VERIFIED.** Client-init feature dump **RUNTIME VERIFIED**. Play-state probes **not live-tested**. |
| 1.21.11 | 21 | shipped | **BUILD VERIFIED.** Play-state probes **not live-tested**. |
| 26.2 | 25 | shipped | **BUILD VERIFIED.** Runtime protocol probe **NOT_SUPPORTED**. |

Do not treat any version as fully **RUNTIME VERIFIED**.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.16.0+** (0.18.5+ for Minecraft 26.1.x) for your Minecraft version.
2. Install matching [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download the `nofingerprint-…+v1.1.7.1-nofingerprint.2-beta.1.jar` that matches your Minecraft version.
4. Place both mods in `.minecraft/mods` and launch.

If you previously used OpSec, the first launch copies `config/opsec.json` and `config/opsec-accounts.json` to `config/nofingerprint.json` and `config/nofingerprint-accounts.json` when the new files do not already exist. After that, NoFingerprint only uses its own filenames.

## Configuration

Open **NoFingerprint** from the multiplayer screen header or [Mod Menu](https://modrinth.com/mod/modmenu). Reconnect after changing protection settings.

Most important settings:

| Setting | Default | Meaning |
|---------|---------|---------|
| **Spoof as Vanilla** | Off | On: vanilla brand and block all mod channels/packs. Off: Fabric brand; whitelist still applies. |
| **Whitelist mode** | AUTO | AUTO allows mods that register network channels. That can reveal those mods. |
| **Isolate pack cache** | On | Per-account UUID cache directories under `downloads/`. |
| **Block local pack URLs** | On | Refuse local/private/CGNAT/ULA pack URLs and unsafe redirects. Stood down if ExploitPreventer is loaded. |
| **Key resolution spoofing** | On | Mitigate translation/keybind probes. Stood down if ExploitPreventer is loaded. |
| **Chat signing** | ON (vanilla-like) | OFF strips signatures; AUTO signs only when the server requires it. |
| **Disable telemetry** | On | Blocks Mojang telemetry send. |

The `/nofingerprint` debug command is **off** by default.

## Security

See [SECURITY_AUDIT.md](SECURITY_AUDIT.md).

**Account tokens are currently stored unencrypted on disk** (`config/nofingerprint-accounts.json`). Anyone with filesystem access to that file can hijack saved accounts. Do not export account JSON to untrusted locations.

Probe diagnostics (`-Dnofingerprint.debug.probes=true`) are **off** by default and must not be enabled in everyday play.

## Developer tooling

`tools/probe-server/` is a local protocol probe harness. It is **not** bundled in the mod JAR. See that directory’s README.

## Upstream / Attribution

NoFingerprint is based on OpSec by aurickk.

Original project: https://github.com/aurickk/OpSec

Keep GPL attribution. See `NOTICE` and `LICENSE`.

## Building from source

Java **17** (1.20.1–1.20.4), **21** (1.20.6–1.21.11), **25** (26.1+). Gradle wrapper included.

```bash
./gradlew :1.20.1:build
./gradlew :1.21.11:build
./gradlew :26.2:build
```

JARs land in `versions/<minecraft_version>/build/libs/`.

## Disclaimer

NoFingerprint is a privacy tool. It is not intended for bypassing server rules, evading bans, or gaining unfair advantages. Users are responsible for the rules and terms of any server they join.
