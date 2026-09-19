# Minecraft Client Fingerprint Surfaces

Research date: **2026-09-19**.  
Subject client: NoFingerprint 1.1.7.1-nofingerprint.1 (Fabric, Stonecutter 1.20.1–26.2).

This catalogue lists ways a Minecraft **server** (or an attacker-controlled resource-pack HTTP host cooperating with that server) can distinguish a modified client from a typical vanilla Java client. It is not a claim that every surface is used in the wild.

Confidence:

- **CONFIRMED** — protocol field or echo path exists; code inspected
- **LIKELY** — protocol exists; observation is indirect but realistic
- **THEORETICAL** — possible with extra assumptions (timing, collusion, future packets)
- **UNKNOWN** — not verified

NoFingerprint protection is stated for the **current** implementation, including default settings (`spoofAsVanilla = false`).

---

## Explicitly disclosed data

### Client brand (`minecraft:brand` / `BrandPayload`)

- What server sends: nothing required; client sends unsolicited (login/config/play depending on version).
- What client does: `ClientBrandRetriever.getClientModName()` → Fabric Loader returns `"fabric"`; vanilla returns `"vanilla"`.
- What server can observe: the UTF string.
- What this can reveal: loader family (Fabric/Quilt/Forge/Lunar/etc.) if the client is honest.
- NoFingerprint: **FULL** only when `spoofAsVanilla` is on (`ClientBrandRetrieverMixin`). Default is unprotected (honest Fabric). Disabled entirely if ExploitPreventer is loaded.
- Versions: all supported.
- Confidence: **CONFIRMED**

### Locale (ClientInformation)

- What server sends: nothing; client sends `ServerboundClientInformation` (name varies).
- What client does: sends selected language tag (`en_us`, …).
- What server can observe: locale string.
- What this can reveal: coarse geography / language pack; not installed mods.
- NoFingerprint: **NO**
- Versions: all.
- Confidence: **CONFIRMED** (field exists). Fingerprint strength: **LIKELY** weak.

### Other client settings

- What client does: view distance, chat visibility, chat colors, skin model bitmask, main hand, text-filtering boolean, allow-server-listings, particle status (newer versions).
- What server can observe: those fields.
- What this can reveal: a small settings tuple; not a mod list.
- NoFingerprint: **NO**
- Confidence: **CONFIRMED** fields; **LIKELY** low uniqueness.

### Chat signing capability

- What server sends: `enforcesSecureChat` / server data; may reject unsigned chat.
- What client does: may send profile public key (`ServerboundChatSessionUpdate`) and `MessageSignature` on chat/commands.
- What server can observe: presence/absence of key; whether messages verify.
- What this can reveal: NCR/NPE/NF-OFF vs default vanilla Java (which signs when a key exists).
- NoFingerprint: **PARTIAL** — SIGN (default, vanilla-like) or OFF (strips signatures, does not suppress key). Stands down if NCR/NPE present.
- Versions: 1.19+.
- Confidence: **CONFIRMED**

### Custom payload identifiers (C2S)

- What server sends: optional S2C probes.
- What client does: sends `ServerboundCustomPayloadPacket` with an id (`fabric:…`, `voicechat:…`, `minecraft:register`, …).
- What server can observe: ids and payloads.
- What this can reveal: specific mods.
- NoFingerprint: **FULL** drop in vanilla mode; **PARTIAL** whitelist in fabric mode (`ClientConnectionMixin`).
- Versions: all; typed payloads 1.20.5+.
- Confidence: **CONFIRMED**

### Registered channel list

- What client does: `minecraft:register` listing plugin channels.
- What server can observe: the list (classic Bukkit/Fabric detection).
- What this can reveal: installed networking mods, often including Fabric API modules.
- NoFingerprint: fabric mode rewrites the list through `ModRegistry.isWhitelistedChannel`; vanilla mode blocks the packet.
- Confidence: **CONFIRMED**

---

## Protocol capability fingerprinting

### Known packs

- What server sends: configuration known-pack offer (1.20.5+).
- What client does: `ServerboundSelectKnownPacks` with `(namespace, id, version)` triples it already has.
- What server can observe: which triples matched.
- What this can reveal: Fabric resource-loader mod packs (notably 1.21.11+ when `ModPackResourcesUtil` exists).
- NoFingerprint: **VERSION LIMITED** — mixin `KnownPacksManagerMixin` ≥1.20.5; effective when Fabric hook present (`ModRegistry.isKnownPacksHookPresent()`). Keeps `minecraft` namespace.
- Confidence: **CONFIRMED** on 1.21.11+ Fabric; **NOT APPLICABLE** before 1.20.5.

### Payload codec support (malformed S2C)

- What server sends: a custom payload on a Fabric channel with illegal bytes (e.g. empty `fabric:registry/sync`).
- What client does: vanilla → `DiscardedPayload` / ignore. Fabric with registered codec → `DecoderException` / disconnect or log.
- What server can observe: disconnect, error, or a different reply vs silence.
- What this can reveal: Fabric networking present **even if register was hidden**, if the inbound codec still runs.
- NoFingerprint: **VERSION LIMITED** — `PayloadTypeRegistryImplMixin` returns null while decoding; `AbstractChanneledNetworkAddonMixin` returns no-handler. ≥1.20.5 only.
- Confidence: **CONFIRMED** (NF comments and Fabric codec behavior). Live disconnect codes **NEEDS TESTING**.

### Resource-pack protocol era

- 1.20.1: single pack on play listener.
- 1.20.2: pack handler moves to common listener; still single pack.
- 1.20.3+: push/pop with UUID, stacked packs, `DownloadQueue`.
- What server can observe: which pack packets the client understands (by joining with that protocol — the version is already known).
- NoFingerprint: version-specific mixins exist; not a hide, a compatibility matrix.
- Confidence: **CONFIRMED**

### HTTP redirect handling on pack download

- What server/pack host sends: 301/302/305/307/308 Location.
- What client does: vanilla `HttpURLConnection` follow; NF reimplements a subset (no 308).
- What observer can see: whether 308 is followed; hop count; TCP to the final host.
- NoFingerprint: **PARTIAL**
- Confidence: **LIKELY** for 308 difference; **THEORETICAL** as a deployed detector.

---

## Mod-specific disclosures

### Fabric API / Fabric Loader

- Channels, brand `fabric`, registry-sync, known-packs, translation keys under `fabric.*` / module ids.
- NoFingerprint: brand opt-in; channels/known-packs/translations filtered by mode; inbound codec ≥1.20.5.
- Confidence: **CONFIRMED**

### Meteor Client

- Old `AbstractSignEditScreenMixin` converted translations to literals of the **key**, an anti-spoof oracle.
- Channels / GUI key `key.meteor-client.open-gui`.
- NoFingerprint: MixinSquared cancel of that mixin `<26.1`; translation/keybind filter; channels depend on whitelist.
- Confidence: **CONFIRMED** for the sign mixin problem; **LIKELY** for channels.

### Voice chat (Simple Voice Chat et al.)

- Persistent custom payloads and register entries.
- NoFingerprint: dropped unless whitelisted. Using the mod on a server **requires** leaking the channel.
- Confidence: **CONFIRMED**

### Xaero / minimaps

- Translation/keybind names that do not use `key.*` prefix; optional channels.
- NoFingerprint: keybind component path covers any name; translations covered if packet-originated.
- Confidence: **LIKELY**

### Essential, ViaFabric/ViaFabricPlus, inventory sync, utility clients

- Dedicated channels and sometimes login queries.
- NoFingerprint: outbound filter; no login-query filter; Via protocol translation is itself a fingerprint (out of scope to "fix").
- Confidence: **LIKELY**

### Quilt

- Brand typically `quilt` if honest.
- NoFingerprint: **NO** dedicated Quilt mode.
- Confidence: **LIKELY** (not tested on Quilt).

---

## Resource-pack fingerprinting

### Shared download cache (cross-account)

- What server sends: the same pack URL+hash to two accounts.
- What client does: vanilla stores under `downloads/` (1.20.3+) or `server-resource-packs/`.
- What server can observe: second account applies instantly / no HTTP GET vs first account's download time.
- What this can reveal: same disk / same player switching accounts.
- NoFingerprint: **FULL** path isolation per account UUID when enabled and ExploitPreventer is not loaded (`DownloadQueueMixin` / `LegacyDownloadedPackSourceMixin`).
- Confidence: **CONFIRMED** for the cache-hit theory; **NEEDS TESTING** for timing thresholds.

### Same-account persistence

- Same as above for one UUID over weeks.
- NoFingerprint: **NO** (no per-session cache).
- Confidence: **CONFIRMED**

### Pack status packets

- What client does: ACCEPTED / SUCCESSFULLY_LOADED / FAILED_DOWNLOAD / FAILED_RELOAD / DECLINED / INVALID_URL / …
- What server can observe: the enum.
- What this can reveal: whether the client blocked a URL, declined required packs, or hashed mismatch.
- NoFingerprint: does **not** spoof status. Lang-only bypass still reports success after download. Local URL block throws → failed download.
- Confidence: **CONFIRMED**

### TrackPack-style burst (many hashes, few URLs)

- What server sends: many push packets.
- What client does: NF `TrackPackDetector` alerts the **user**; it does not change protocol.
- Confidence: **LIKELY** as an attack pattern; detector is UX only.

### HTTP headers / TLS / ETag

- Attacker-controlled pack host sees Java `HttpURLConnection` fingerprint.
- NoFingerprint: unmodified besides redirect wrapper.
- Confidence: **THEORETICAL** vs other Java clients; **UNLIKELY** vs vanilla Java on the same JDK.

---

## Translation/component fingerprinting

### Sign and anvil echo (primary)

- What server sends: `TranslatableContents` / `KeybindContents` on a sign or anvil item name.
- What client does: opens editor; vanilla `getString()` resolves using loaded language (mods + packs).
- What server can observe: the text the client sends back.
- What this can reveal: whether a translation key exists (mod installed) and sometimes the bound key.
- NoFingerprint: **FULL** for packet-originated components (`NoFingerprintComponentCodec` + `TranslatableContentsMixin` + `KeybindContentsMixin`). Returns vanilla/pack/fallback, not live mod language.
- Versions: all; codec wrap ≥1.20.5, Gson mark ≤1.20.4.
- Confidence: **CONFIRMED**

### Display-only components (chat, title, boss bar, scoreboard, hover, …)

- What server can observe: nothing unless the player reports it or another packet echoes it.
- NoFingerprint still marks them so a later echo is safe.
- Confidence of **server** exploit: **THEORETICAL**. Protection: **FULL** on the resolve path.

### Item lore / data components

- May be re-serialized on some inventory actions.
- NoFingerprint: marked if decoded via `ComponentSerialization`.
- Confidence of leak: **LIKELY** on some versions; **NEEDS TESTING**.

### Server-pack supplied keys

- Pack lang files are allowed through (`getServerPackTranslation`). A vanilla client with that pack would resolve the same.
- Confidence: **CONFIRMED** intended.

---

## Behavioral fingerprinting

| Behavior | Observable? | NF vs vanilla | Confidence |
|---|---|---|---|
| Extra Netty handler | Not directly | Present 1.20.2+ | THEORETICAL |
| Dropped custom payloads (no response) | Missing traffic | Vanilla mode | CONFIRMED |
| Immediate FAILED on RFC1918 pack URL | Status + timing | Faster than vanilla/EP dummy Socket | LIKELY |
| Unsigned chat | Chat packet | OFF mode | CONFIRMED |
| Lang-only pack, textures missing | Gameplay / anti-cheat, not pack status | SUCCESS still sent | LIKELY for AC, not for pack protocol |
| Meteor sign mixin cancelled | Sign echo | Closer to vanilla than stock Meteor | CONFIRMED |
| Default brand `fabric` | Brand packet | Same as honest Fabric | CONFIRMED |

---

## Timing-based fingerprinting

- Pack cache hit vs miss (cross-account): mitigated by UUID dirs.
- Local URL: NF throws without connecting (except max-redirect dummy socket). Vanilla connects. EP connects then throws. **LIKELY** distinguishable with a cooperating local listener **or** RTT of FAILED.
- Chat signing CPU: negligible.
- Translation resolve: extra mixin work; **THEORETICAL**.
- Configuration-phase duration: **UNKNOWN**.

---

## Error-based fingerprinting

| Probe | Vanilla | Honest Fabric | NF vanilla/fabric spoof |
|---|---|---|---|
| Malformed Fabric S2C codec | Ignore | DecoderException | Ignore if inbound filter on (≥1.20.5) |
| Unknown channel | Ignore | Ignore or handler | Ignore if dropped |
| Invalid pack URL | INVALID_URL / fail | Same | Same, plus local-address IllegalStateException internally |
| Too many redirects | ProtocolException | Same | Same, with dummy Socket |
| Login query (plugin) | Responds | Responds | Responds (Rebrand may null) |

Confidence: **CONFIRMED** for the Fabric codec story in source; live disconnect codes **NEEDS TESTING**.

---

## Version-specific surfaces

| Version | Surfaces that appear or change |
|---|---|
| 1.19+ | Chat keys/signatures |
| 1.20.1 | No config phase; custom payload in `protocol.game`; **no NF local-URL mixin**; legacy pack cache isolation |
| 1.20.2 | Config phase; common custom payloads; HttpUtil redirect hook starts |
| 1.20.3 | Multi-pack UUID push/pop; `DownloadQueue` isolation |
| 1.20.5 | Typed payloads; `ComponentSerialization` codec wrap; `KnownPacksManager` mixin compiled; inbound Fabric codec gate |
| 1.21.4 | Deprecated translation key sync mixin |
| 1.21.9 | `PacketProcessor` handle wrap (replaces PacketUtils path) |
| 1.21.11 | Fabric known-pack hook typically present; `Identifier` rename |
| 26.1+ | Meteor sign mixin gone; key mapping registry package rename |
| 26.2 | Current newest NF target |

---

## Unknown / research needed

- Whether 26.x known-pack records gained fields beyond `(namespace, id, version)`.
- Item-component echo of resolved text on 1.20.5–26.2.
- Velocity modern forwarding vs login query nulling (Rebrand).
- IPv4-mapped IPv6 and decimal-IPv4 hosts in Java `URL`/`InetAddress` on JDK 17 vs 21 vs 25.
- Whether `minecraft:mco` drop in NF fabric mode matches stock Fabric on every version.
- Quilt / NeoForge clients (NF is Fabric-only).
- Closed-source server plugins that claim "mod detection" without publishing probes.
- TLS/HTTP2 vs HTTP1.1 for pack CDNs.

---

## Summary: what a server actually gets from a default NoFingerprint install

CODE VERIFIED defaults: `spoofAsVanilla = false`, translation protection on, fake default keybinds on, isolate cache on, block local URLs on (not 1.20.1), strip mod shaders on, signing SIGN, telemetry off.

The server still sees **Fabric brand** and **whitelisted (AUTO) mod channels**. It should **not** see sign/anvil translation oracles, shared-cache cross-account hits, or RFC1918 pack fetches (1.20.2+). That is a privacy hardening profile, not a vanilla clone.
