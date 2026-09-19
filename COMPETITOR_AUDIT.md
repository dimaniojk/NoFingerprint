# NoFingerprint Competitor Audit

Research date: **2026-09-19**.  
NoFingerprint version audited: **1.1.7.1-nofingerprint.1** (`io.github.dimaniojk.nofingerprint`).  
Fork of aurickk/OpSec. This document is a code-level audit. It is not a README comparison and does not claim NoFingerprint is undetectable.

**No protections were implemented in this pass.** Findings are for a later implementation review.

## Phase 1 follow-up (1.1.7.1-nofingerprint.2)

The tables and method-level notes below remain the **nofingerprint.1** snapshot. They were not rewritten in place. Status of issues this fork actually changed:

| Historical finding | Status | What changed |
|---|---|---|
| EP stand-down disabled brand/channels/known-packs/cache/pack-strip | **RESOLVED** | `CompatibilityPolicy` keeps those features. Only HTTP URL wrap and translation/component wrap stand down (unsafe dual Mixins). `fabric.mod.json` `conflicts` removed so dual-install is an intended coexistence path. |
| IPv6 ULA / CGNAT / mapped IPv6 gaps | **RESOLVED** for NoFingerprint's own classifier | `PrivateAddressClassifier` uses explicit CIDR. EP's classifier is still the weaker one when EP is installed (NF stands down the HTTP hook). |
| HTTP 308 omitted | **RESOLVED** | Follows 300–303/305/307/**308**. 304/306 are not redirects. |
| 1.20.1 local URL stub | **RESOLVED** | `HttpUtilMixin` targets `method_15303` / `lambda$downloadTo$0` on 1.20.1–1.20.2; `downloadFile` on 1.20.3+. |
| DNS TOCTOU / rebinding | **PARTIALLY RESOLVED** | All A/AAAA at check time; every redirect hop re-checked before `openConnection`. Connected peer is **not** pinned. Status: **PARTIALLY MITIGATED**, not FIXED. Do not claim DNS-rebinding protection. |
| Unusual IPv4 spellings (`127.1`, decimal, hex) | **N/A** | This JDK stores those tokens as hostnames; no custom parser was added. |

EP itself was not patched. Dual-install URL blocking still uses EP's `isSiteLocalAddress` classifier.

## Scope

Inspected:

- The current NoFingerprint source tree, Mixins, Stonecutter version matrix, `fabric.mod.json`, and `SECURITY_AUDIT.md`.
- Upstream OpSec (`https://github.com/aurickk/OpSec`, cloned 2026-09-19).
- Open-source competitors cloned into `/tmp/nf-audit` (not committed).
- GitHub/Modrinth only to locate repositories. Feature claims below are **code-verified** unless marked **DOCUMENTATION CLAIM** or **UNKNOWN**.

Minecraft versions considered for NoFingerprint: Stonecutter targets **1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.6, 1.21.9, 1.21.11, 26.1, 26.2**. Representative remap builds were previously reported for 1.20.1, 1.21.11, and 26.2. This audit did **not** re-run live Minecraft clients.

Evidence classes used throughout:

| Label | Meaning |
|---|---|
| **CODE VERIFIED** | Behavior read from source (class/mixin/method cited). |
| **DOCUMENTATION CLAIM** | README/Modrinth text that was not confirmed in source, or that contradicts source. |
| **THEORETICAL / NEEDS TESTING** | Plausible protocol/OS behavior without a live packet capture. |

README marketing was treated as a hypothesis, not evidence. ExploitPreventer's README still describes translation protection as sign/anvil-only; the current source wraps `ComponentSerialization` for all packet-originated components. Client Spoofer's README claims "hides your mod list"; the source only spoofs brand, filters some outbound custom payloads, filters `fabric` known-packs, isolates pack cache, and sanitizes sign/anvil `getString()`.

---

## Projects analyzed

### 1. NoFingerprint (this tree)

- URL: https://github.com/dimaniojk/NoFingerprint (declared in `fabric.mod.json`; hosting may still be incomplete)
- Source: this workspace
- License: **GPL-3.0-only**
- Loaders: Fabric client
- Minecraft: Stonecutter 1.20.1–26.2 as listed above
- Last activity: current fork
- Source available: **yes**

### 2. OpSec (upstream)

- URL / source: https://github.com/aurickk/OpSec
- License: **GPL-3.0**
- Loaders: Fabric client
- Minecraft: same Stonecutter matrix as this fork (1.20.1–26.2)
- Last activity: cloned 2026-09-19; still the lineage NoFingerprint was rebranded from
- Source available: **yes**
- Notes: NoFingerprint is a rebrand, not a rewrite. Functional differences vs this tree are small (package/id/config filenames). Treat OpSec and NoFingerprint as the same protection design unless a file-level difference is cited.

### 3. ExploitPreventer

- URL: https://modrinth.com/mod/exploitpreventer
- Source: https://github.com/NikOverflow/ExploitPreventer
- License: **MIT**
- Loaders: Fabric client
- Minecraft: Stonecutter versions present for **1.21.9, 1.21.11, 26.1, 26.2, 26.3**
- Last activity: CHANGELOG mentions 26.3 support
- Source available: **yes**
- Explicit non-goals in source/README: does **not** spoof brand, hide channels, or hide mods. CODE VERIFIED: no `ClientBrandRetriever` mixin, no outbound payload filter.

### 4. Client Spoofer

- URL: https://modrinth.com/mod/clientspoofer
- Source: https://github.com/FabiPunktExe/ClientSpoofer
- License: **MIT**
- Loaders: Fabric client
- Minecraft: cloned default branch targets **26.1** (`gradle/libs.versions.toml`)
- Last activity: 26.1 branch
- Source available: **yes**

### 5. Spoofified

- URL / source: https://github.com/igameshat/spoofified
- License: **MIT**
- Loaders: Fabric client
- Minecraft: `fabric.mod.json` depends on `~26.2`
- Last activity: Client Spoofer fork with extra local UI hiding
- Source available: **yes**
- Notes: README still talks about "Client Spoofer" config `config/clientspoofer.json`. Extra mixins hide Mod Menu entries, keybind list rows, and client commands. Those are **local UI**, not server-observable.

### 6. Debrand (haykam821) — active Fabric "Debrand"

- URL / source: https://github.com/haykam821/Debrand
- License: **MIT**
- Loaders: Fabric client
- Minecraft: `gradle.properties` **1.20.1**
- Source available: **yes**
- CODE VERIFIED: window title / title-screen "modded" chrome only (`MinecraftClientMixin.getWindowTitle`, `TitleScreenMixin`). **No server brand mixin.** Not a fingerprinting competitor.

### 7. debrand (LoganDark)

- URL / source: https://github.com/LoganDark/debrand
- License: **MIT** (`LICENSE.md`)
- Loaders: Fabric (template-looking package `TEMPLATE_PACKAGE`)
- Minecraft: `gradle.properties` **26.3-rc-2**
- Source available: **yes**, but the tree looks like an unfinished template. `MixinClientBrandRetriever` always returns `"vanilla"`.

### 8. Rebrand

- URL: https://modrinth.com/mod/rebrand
- Source: https://github.com/kaylendog/rebrand
- License: **GPL-3.0-or-later**
- Loaders: Fabric client
- Version in tree: **2.0.5**
- Source available: **yes**
- CODE VERIFIED extras: `LoginQueryResponseC2SPacketMixin` (login plugin-message responses) and outbound custom-payload filtering. Control-flow vs comments is inconsistent (see Networking).

### 9. NoFabric

- URL: https://modrinth.com/mod/nofabric (listed); source https://github.com/qtchaos/NoFabric
- License: **GPL-3.0**
- Loaders: Fabric
- Minecraft: branch/default **1.21.1**
- Last push observed in GitHub metadata: 2024-10
- Source available: **yes** — a **single mixin**
- CODE VERIFIED: cancels **every** `CustomPayloadC2SPacket`, including `minecraft:brand`. That is **not** vanilla behavior.

### 10. Custom Client Brand

- URL: https://modrinth.com/mod/custom-client-brand
- Source: https://github.com/MrKinau/CustomClientBrandMod
- License: **CC0-1.0**
- Loaders: Fabric
- Minecraft: `gradle.properties` **26.1**
- Source available: **yes**
- CODE VERIFIED: only `BrandPayload.write` rewrite. No channel, translation, or pack logic.

### 11. No Chat Reports

- URL: https://modrinth.com/mod/no-chat-reports
- Source: https://github.com/Aizistral-Studios/No-Chat-Reports (branch `26.2-Unified`)
- License: **WTFPL**
- Loaders: Fabric / Forge / NeoForge; client and/or server
- Minecraft: **26.2** in this clone
- Source available: **yes**
- Scope for this audit: chat signing, secure-chat UX, telemetry. Not a general anti-fingerprint mod.

### 12. No Prying Eyes

- URL: https://modrinth.com/mod/no-prying-eyes
- Source: https://github.com/Daxanius/NoPryingEyes
- License: **MIT**
- Loaders: Fabric / NeoForge (Architectury-style `common/` + `fabric/`)
- Minecraft: **26.2** in this clone
- Source available: **yes**
- Scope: telemetry dummy sender, chat-signing modes including **NO_KEY** (suppress profile key pair), plus unrelated extras (fake ban, profanity filter, "UK chat restriction" patch) that are **out of scope** for fingerprinting.

### Additional projects discovered

| Project | Source | License | Why included |
|---|---|---|---|
| Fabric Hider | https://github.com/trolltaylor/fabric-hider | **CC0-1.0** | Brand spoof + F3/log hiding. Last real activity ~1.18.2. |
| IceCream | https://github.com/Bawnorton/IceCream | **MIT** | Brand-only "flavour" picker (vanilla/fabric/forge/lunar). Targets 1.19.4. |
| LiquidBounce `MixinDownloadQueue` | https://github.com/CCBlueX/LiquidBounce (not fully cloned) | GPL family (not re-copied here) | Origin of per-account pack cache; already attributed in `DownloadQueueMixin`. |

No Quilt-specific anti-fingerprint project was found. No competitor modified `ClientInformation` / locale packets.

---

## Feature matrix

Statuses: **FULL** / **PARTIAL** / **NO** / **UNKNOWN** / **VERSION LIMITED** / **NOT APPLICABLE**.

Abbreviations: NF = NoFingerprint, EP = ExploitPreventer, CS = Client Spoofer, SF = Spoofified, RB = Rebrand, NFab = NoFabric, CCB = Custom Client Brand, NCR = No Chat Reports, NPE = No Prying Eyes, FH = Fabric Hider, IC = IceCream, DH = haykam Debrand.

| Surface | NF | EP | CS | SF | RB | NFab | CCB | NCR | NPE | FH | IC | DH |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Client brand spoofing | FULL | NO | FULL | FULL | FULL | NO | FULL | NO | NO | FULL | FULL | NO |
| Fabric loader identification protection | PARTIAL | NO | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | PARTIAL | NO | NO |
| Quilt loader identification protection | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Custom payload filtering | PARTIAL | NO | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO |
| Outgoing payload filtering | FULL | NO | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO |
| Incoming payload filtering | VERSION LIMITED | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Registered channel hiding | FULL | NO | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO |
| Fabric networking channel hiding | VERSION LIMITED | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Fabric handshake suppression | PARTIAL | NO | NO | NO | PARTIAL | NO | NO | NO | NO | NO | NO | NO |
| Mod-list leakage protection | PARTIAL | NO | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO |
| Known-pack filtering | VERSION LIMITED | NO | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Translation-key probing protection | FULL | FULL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Keybind probing protection | FULL | FULL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Component-resolution protection | FULL | FULL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Resource-pack fingerprint protection | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Per-account pack cache isolation | FULL | FULL | FULL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Per-session cache isolation | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Resource-pack status spoofing | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Required-pack bypass behavior | FULL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Localhost resource-pack blocking | VERSION LIMITED | FULL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Private IPv4 blocking | VERSION LIMITED | FULL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Private IPv6 blocking | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Link-local blocking | VERSION LIMITED | FULL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| IPv6 ULA blocking | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| IPv4-mapped IPv6 handling | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Redirect validation | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| DNS resolution validation | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| DNS rebinding resistance | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Shader override protection | FULL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Server-controlled URL protection | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Telemetry blocking | FULL | NO | NO | NO | NO | NO | NO | FULL | FULL | NO | NO | NO |
| Chat signing privacy | PARTIAL | NO | NO | NO | NO | NO | NO | FULL | FULL | NO | NO | NO |
| Locale fingerprint protection | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Client settings fingerprint protection | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Account/session isolation | PARTIAL | PARTIAL | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO |
| Mod-specific compatibility | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NOT APPLICABLE | NOT APPLICABLE | NO | NO | NO |
| Meteor detection mitigation | VERSION LIMITED | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Error-response fingerprinting protection | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Malformed-packet fingerprint protection | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Timing/behavior fingerprint mitigation | PARTIAL | PARTIAL | NO | NO | NO | NO | NO | NO | NO | NO | NO | NO |
| Version-specific protocol protection | FULL | PARTIAL | NO | NO | NO | NO | NO | PARTIAL | PARTIAL | NO | NO | NO |

PARTIAL / UNKNOWN / VERSION LIMITED explanations are in the sections below.

---

## Current NoFingerprint coverage

Every protection below was verified in this tree. Stonecutter `//? if` gates are part of the behavior.

### Client brand spoofing

- Class: `mixin/client/ClientBrandRetrieverMixin`
- Target: `ClientBrandRetriever.getClientModName` (`remap = false`)
- Method: `@Inject HEAD` cancellable, returns `config.getSettings().getEffectiveBrand()`
- Runtime gate: `NoFingerprintConfig.shouldSpoofBrand()` = `!EXPLOIT_PREVENTER_LOADED && settings.isSpoofAsVanilla()`
- Default: **`spoofAsVanilla = false`**. Out of the box the advertised brand is still Fabric unless the user opts in.
- Assumptions: servers read `minecraft:brand` / `BrandPayload` from this retriever. True for vanilla/Fabric.
- Weakness: brand-only is insufficient; Fabric still registers channels unless channel spoofing is on. Coexistence with ExploitPreventer **disables this entirely** even though EP does not spoof brand.
- Compatibility: vanilla-brand + remaining Fabric channels is an inconsistent identity.

### Outgoing custom payloads / channel registration

- Class: `mixin/client/ClientConnectionMixin`
- Packets: `ServerboundCustomPayloadPacket` (1.20.2+ `common`; 1.20.1 `game` + raw `FriendlyByteBuf`)
- Methods: `send(Packet)` HEAD cancel; 1.20.2+ also installs Netty handler `nofingerprint_filter` after `encoder`
- Vanilla mode: drop every custom payload except `BrandPayload` / `minecraft:brand`
- Fabric mode: rewrite `minecraft:register`/`unregister` to whitelisted channels via `ModRegistry.isWhitelistedChannel`; drop non-whitelisted payloads; specially drop `minecraft:mco` "to match stock Fabric"
- 1.20.5+: inspects Fabric `RegistrationPayload`
- Assumptions: all mod identity on the wire is a custom payload or register list. False for known-packs, translations, resource-pack cache, chat signing, ClientInformation.
- Weakness: pipeline + mixin duplication; if encoder path bypasses `send()`, the Netty handler is the backstop (1.20.2+ only).
- Compatibility: vanilla mode breaks Simple Voice Chat, Xaero networking, ViaFabricPlus extra channels, Essential, inventory sync mods, etc.

### Incoming Fabric payload / handshake fingerprint

- Decode: `mixin/client/PayloadTypeRegistryImplMixin` into `PayloadTypeRegistryImpl.get(id)` while `PacketContext.isDecodingPayload()`
- Dispatch: `mixin/client/AbstractChanneledNetworkAddonMixin` into `AbstractChanneledNetworkAddon.handle`, client addons only
- Shared policy: `protection/ClientSpoofer.shouldDropInboundChannel` — `minecraft:` always kept; vanilla mode drops all other namespaces; fabric mode drops non-whitelisted
- Gate: **`>=1.20.5`**. Below that, typed payloads / this registry do not exist; unknown channels are already ignored by vanilla.
- Purpose: avoid Fabric's strict codec throwing `DecoderException` on a malformed `fabric:registry/sync` (or similar) probe, which is a **presence oracle**.
- Weakness: does not suppress Fabric's own capability advertisement if some other path still sends register lists before the filter; login-phase plugin queries are not covered.
- VERSION LIMITED: no equivalent inbound codec gate on 1.20.1–1.20.4.

### Configuration-phase debug mixin

- `ChannelRegistrationMixin` → `ClientConfigurationPacketListenerImpl.handleConfigurationFinished` (`>=1.20.2`)
- Logs only. Not a protection.

### Known-pack filtering

- Class: `mixin/client/KnownPacksManagerMixin` (`>=1.20.5`)
- Packet/effect: filters the list returned by `KnownPacksManager.trySelectingPacks` (feeds `ServerboundSelectKnownPacks`)
- Keeps `namespace == "minecraft"`; vanilla mode drops everything else; otherwise `ModRegistry.isWhitelistedKnownPack`
- Runtime no-op unless `ModRegistry.isKnownPacksHookPresent()` which probes `net.fabricmc.fabric.impl.resource.pack.ModPackResourcesUtil` (Fabric resource-loader hook shipping **from MC 1.21.11**)
- Absence of this mixin on `<1.20.5` is **not a bug**: the protocol does not exist.
- On 1.20.5–1.21.10 the mixin is compiled but typically does nothing useful because Fabric is not injecting mod packs into the handshake yet.
- Client Spoofer only nulls packs with namespace `"fabric"` — narrower and easier to bypass with a non-`fabric` namespace.

### Translation / component probing

Packet-origin tagging:

- `>=1.20.5`: `ComponentSerializationMixin` wraps `Codec.recursive` with `NoFingerprintComponentCodec`, which marks trees when `PacketContext.isProcessingPacket()` and not singleplayer.
- `<1.20.5`: Gson `Component.Serializer.deserialize` marks every deserialized tree (except singleplayer). Broader than packet-only; can mark some local JSON.
- Context flags: `PacketDecoderMixin` (`>=1.20.5` StreamCodec.decode), `PacketProcessorMixin` (`>=1.21.9` `PacketProcessor$ListenerAndPacket.handle`), `PacketUtilsMixin` (`<1.21.9` `PacketUtils.method_11072`), plus `ClientPacketListenerMixin.handleBundlePacket` for bundle sub-packets.

Resolution:

- `TranslatableContentsMixin` wraps `Language.getOrDefault` inside `decompose`
- Only when `nofingerprint$fromPacket` and not singleplayer
- Vanilla keys pass; vanilla mode blocks other keys (pack-defined value if present, else fallback/key); fabric mode allows `ModRegistry.isWhitelistedTranslationKey`
- `DeprecatedTranslationsInfoMixin` (`>=1.21.4`) keeps the vanilla-key set in sync with removed/renamed keys
- `ClientLanguageMixin` keeps a private language map and records pack translations so blocked keys can still echo **server-pack** values (vanilla-identical for pack-provided keys)

Assumptions: the server can only observe a difference if the client **echoes** resolved text, or if a human reports what they saw. Codec-level marking is still the right defense because echo surfaces are not limited to signs.

Weakness: Mixins on `decompose` miss any future lookup that does not go through `TranslatableContents.decompose`. Hover/nested components that decode through the same `Component` codec **are** marked (recursive decode while `isProcessingPacket` is true). **THEORETICAL:** custom-payload NBT parsed by another mod with a non-vanilla component codec.

### Keybind probing

- `KeybindContentsMixin` wraps `Supplier.get()` in `getNestedComponent`
- Same packet-origin flag
- Whitelisted keybinds resolve for real
- Vanilla keybinds: if `fakeDefaultKeybinds`, return `KeybindDefaults.getDefault(name)` as a literal; else real binding
- Unknown/mod keybinds: `Component.translatable(name)` re-entering the translation pipeline (pack value or raw key)
- `KeyBindingRegistryImplMixin` records Fabric-registered keybinds for whitelist ownership (`KeyBindingRegistryImpl` / 26.1+ `KeyMappingRegistryImpl`)

What it does **not** protect: the actual `KeyMapping` the player uses locally; modifier chords as gameplay; categories in the controls screen (local UI). Server observation still requires an echo surface (`keybind` component resolved then sent back).

### Resource-pack cache isolation

- `>=1.20.3`: `DownloadQueueMixin` `ModifyExpressionValue` on `Path.resolve` inside `lambda$runDownload$0` / `method_55485` → `cacheDir/<accountUuid>/<packId>`
- Skips if another mod already reparented off `cacheDir` (LiquidBounce/Meteor coexistence)
- `<1.20.3`: `LegacyDownloadedPackSourceMixin` rewrites the `File` under `server-resource-packs/<uuid>/`
- Gate: `shouldIsolatePackCache()` also false if ExploitPreventer is loaded
- Does **not** isolate per session, HTTP cache, or temp files outside those directories
- UUID, not username: switching accounts with isolation on uses a different directory. **CODE VERIFIED path layout.** Cross-account correlation via shared cache is the thing this prevents. Other correlation (timing, headers, acceptance policy) remains.

### Local/private URL blocking

- `util/LocalAddressUtil.isLocalAddress`: `InetAddress.getAllByName` then `isAnyLocalAddress || isLoopbackAddress || isSiteLocalAddress || isLinkLocalAddress`
- `HttpUtilMixin` wraps `HttpURLConnection.getInputStream` in `HttpUtil.downloadFile` (`>=1.20.3`) or intermediary `method_15303` (`1.20.2`)
- **1.20.1: stub mixin. `MC_VERSION_HAS_BLOCK_LOCAL_URLS = false`.** VERSION LIMITED.
- Per-hop redirect follow for 300/301/302/303/305/307; **not 308**
- 305 handled as HTTP proxy hop with a fresh `Authenticator` so JDK does not reuse a cached client socket (comment: match vanilla TCP fingerprint)
- Max-redirect path opens a dummy `Socket` then throws `ProtocolException` to match vanilla leak behavior
- **Does not** open a dummy Socket on the initial local-block path (ExploitPreventer does). Timing of FAILED pack status can therefore differ from vanilla/EP.
- Skips blocking when the connected game server itself is local (`LocalAddressUtil.serverAddress`)

IPv6 ULA `fc00::/7` is **not** `isSiteLocalAddress()` in Java. That is a **CODE VERIFIED gap** shared with ExploitPreventer (`Utils.isLocalAddress` is the same four predicates).

### Shader stripping

- `LangOnlyPackResources.shouldStripShader`: any `assets/<ns>/shaders/*` where `ns != minecraft`, `shouldStripModShaders()`, and `!ModRegistry.isServerPackShaderAllowed(ns)`
- Runs even when the pack is "loaded for real" (independent of lang-only filter)
- Vanilla `minecraft` shaders are not stripped
- `ShaderStripTracker` defers chat/toast until the player exists
- Not EP-gated (`shouldStripModShaders` comment: EP lacks this)

Can break legitimate server packs that ship Iris/Sodium/mod shader overrides under a mod namespace. Compatibility risk: **medium** for shader-using clients.

### Required-pack bypass

- `PackStripHandler` + `LangOnlyPackResources` + pack-screen mixins (`PackRepositoryMixin`, `PackSelectionModelMixin`, `PackSelectionModelEntryBaseMixin`, `DownloadedPackSourceMixin`)
- Modes: MANUAL / ASK / ALWAYS_ON
- Download, hash, and status packets still run. The wrapper hides non-lang assets so the server still sees SUCCESS (vanilla-like) while visuals do not apply
- This is **not** status spoofing; it is content stripping after a successful vanilla apply flow
- EP-gated: `shouldStripPack()` is false when ExploitPreventer is loaded (EP has no equivalent)

### Chat signing

- `SpoofSettings.SigningMode`: **SIGN** (default) and **OFF** only
- AUTO / ON_DEMAND were **removed** (comment in `SpoofSettings`: auto-upgrade from attacker-controlled system chat fingerprinted the mod; proxy `enforcesSecureChat` is set by the proxy, not the backend)
- `ServerboundChatPacketMixin`: nulls `signature` on init and on `signature()` getter when `shouldNotSign()`
- `ClientPacketListenerMixin`: wraps `SignedMessageChain.Encoder.pack` in `sendChat` so the chain does not advance
- Stands down if `nochatreports` or `nopryingeyes` is loaded (`CHAT_SIGNING_MANAGED_EXTERNALLY`)
- Does **not** suppress profile key pair send (NPE `NO_KEY` is stronger for "this client has no chat key")
- OFF vs vanilla-with-optional-signing is server-visible on servers that do not require signatures. SIGN is vanilla-like.

### Telemetry

- `MinecraftMixin.allowsTelemetry` → false
- `YggdrasilUserApiServiceMixin.newTelemetrySession` → `TelemetrySession.DISABLED`
- Stands down if NCR or NPE loaded
- Telemetry is Mojang-bound, not a multiplayer-server fingerprint, except insofar as a future server infers "this user disabled telemetry" via other Mojang APIs (not observed here).

### Meteor

- `mixin/MeteorMixinCanceller` (MixinSquared): cancels `meteordevelopment.meteorclient.mixin.AbstractSignEditScreenMixin` when meteor-client is loaded and `meteorFix` is true
- `>=26.1`: no-op; Meteor 26.1.2 removed that mixin
- Reads config from disk at class init (before `NoFingerprintConfig`)
- Remaining Meteor leaks: Meteor network channels if not filtered; Meteor translation keys if protection off; gameplay behavior. Not cancelled.

### Account manager (inherited OpSec)

- `accounts/AccountManager`, `SessionAccount`, `CrackedAccount`
- Persists `accessToken` / `refreshToken` in plaintext JSON (`SessionAccount.toJson`)
- Not a fingerprint protection. Expands secret surface. Classified KEEP for this task (do not remove), REWRITE later for storage.

### ExploitPreventer coexistence (critical)

`fabric.mod.json` has `"conflicts": { "exploitpreventer": "*" }` (warning, not a hard `breaks`). Runtime still detects EP and **disables**:

- brand spoof, channel spoof, known-pack filter, translation protection, pack-cache isolation, local-URL block, whole-pack strip

EP does **not** implement brand/channel/known-pack spoofing. Dual install therefore **drops NF identity protections without EP replacements**. Shader strip remains.

This is the single most important compatibility defect in the current policy model.

---

## Detection surface analysis

### Explicit disclosure

| Signal | Mechanism | NF |
|---|---|---|
| `minecraft:brand` / `BrandPayload` | Client sends brand string during login/config/play | Protected only if `spoofAsVanilla` |
| `minecraft:register` channel list | Client advertises plugin channels | Filtered in vanilla/fabric modes |
| Fabric custom payloads | Mod namespaces on C2S | Dropped when not whitelisted |
| Known packs | `ServerboundSelectKnownPacks` triples | Filtered when Fabric hook present |
| ClientInformation | locale, view distance, chat settings, model parts, main hand, text filtering, server listings, particles (version-dependent) | **Unmodified** |
| Chat session / profile public key | Client sends key + signed messages | SIGN keeps vanilla; OFF strips signatures but still may send key |
| Resource-pack status | SUCCESS/FAILED/DECLINED/ACCEPTED | Not spoofed |
| Secure chat support | `enforcesSecureChat` reaction + signing | SIGN/OFF only |

### Protocol capability differences

- Known-pack packet existence: 1.20.5+. A 1.20.4 client cannot be probed this way.
- Typed custom payload codecs: 1.20.5+. Fabric throwing vs vanilla `DiscardedPayload` is a **confirmed** presence oracle; NF patches this `>=1.20.5`.
- Configuration phase: 1.20.2+. Channel registration timing differs vs 1.20.1.
- Multi-pack push/pop: 1.20.3+.
- Missing 308 redirect handling vs a JDK that would follow 308: **THEORETICAL** capability fingerprint.

### Indirect inference

- Translation/keybind echo (sign editor, anvil output, other getString() round-trips): **CONFIRMED** on sign/anvil; NF + EP cover all packet-originated components, CS/SF only sign/anvil.
- Pack cache hit vs miss (download time, whether HTTP GET happens): isolation reduces **cross-account** hits, not per-account persistence.
- Local URL success/fail/hash: EP/NF block private hosts (with gaps). CS/SF do not.
- DecoderException vs silent ignore on malformed Fabric payload: NF inbound filter.

### Behavioral differences

- Packet ordering / extra Netty handler `nofingerprint_filter`
- Immediate fail on local URL vs vanilla TCP-then-fail (NF vs vanilla/EP)
- Chat unsigned when surrounding vanilla clients sign
- Lang-only pack: SUCCESS with missing textures — server cannot see textures, but **gameplay/resource-dependent anti-cheat** can (out of scope)
- Meteor mixin cancellation changes sign-edit echo vs stock Meteor

### Implementation bugs (competitor or NF)

- NoFabric drops brand entirely (not vanilla).
- Rebrand ghost-mode control flow vs comments: when `ghostMode` is true, channel filter **does not run**; when false, it does. Login-query nulling uses the same inverted-looking gate.
- Spoofified `DownloadQueueMixin`: isolates cache when `preventFingerprinting() || !ENABLED` — disabled mod still isolates (logic bug).
- NF+EP dual load disables NF identity spoofing.

### Unknown / research-needed

- Registry-sync extra fields beyond Fabric codec throw
- Data-component custom codecs in items that never pass `ComponentSerialization`
- Recipe book / command-suggestion differences that are server-visible
- ViaVersion/ViaFabric protocol translation fingerprints
- HTTP `User-Agent` / TLS JA3 of Java `HttpURLConnection` vs vanilla (should match if the same JVM)

---

## Translation / component probing

Vanilla resolves `TranslatableContents` in `decompose()` via `Language.getOrDefault`. `KeybindContents.getNestedComponent()` resolves through `KeyMapping.createNameSupplier`.

**Observation mechanism required.** Display-only surfaces (chat, title, boss bar, scoreboard, hover) are **not** exploitable by the server unless the client sends the resolved string back or a player is social-engineered.

### Surfaces

| Surface | Server-observable? | Confidence | Notes |
|---|---|---|---|
| Sign editor | Yes — client sends resolved lines | CONFIRMED | Classic vector. CS/SF mixin `AbstractSignEditScreen`. NF/EP codec+decompose. Meteor's old mixin returned raw keys (anti-spoof oracle); NF cancels it. |
| Anvil / item rename | Yes — result name packet | CONFIRMED | CS `AnvilMenuMixin`/`AnvilScreenMixin`. NF/EP cover via component mark. |
| Books (unsigned display) | Usually no | THEORETICAL | Signing sends player-typed pages, not automatic translation echo. |
| Chat / system / action bar / title | No unless echo or screenshot | THEORETICAL | Still marked by NF/EP so a future echo path is covered. |
| Boss bar / scoreboard / team / entity names | Same | THEORETICAL | |
| Item names / lore / data components | Depends if the client re-serializes resolved text | LIKELY for some inventory actions | Codec path should mark them. **NEEDS TESTING** on 1.20.5+ item components. |
| Advancements / recipes / death messages | Display-only | THEORETICAL | |
| Command suggestions | Client-local for client commands | LIKELY none | Spoofified hides client commands locally only. |
| Hover / click events | Nested decode through same codec | LIKELY protected in NF/EP | |
| Custom payload components | If a mod parses JSON/NBT itself | UNKNOWN | |
| Server resource packs | Pack lang can **supply** keys; that is vanilla-identical and must be allowed | CONFIRMED | NF uses `ModRegistry.getServerPackTranslation` so pack keys echo like vanilla. |

### Comparison

- **ExploitPreventer**: `ComponentCodec` + `ComponentMarker` on `TranslatableContents`/`KeybindContents`; `PacketDecoderMixin` sets `isFromPacket`; `TranslationFilter` uses vanilla-only `ClientLanguage` for marked lookups. Closest peer to NF. NF additionally fabric/vanilla whitelist modes, pack-value echo, alerts, Meteor cancel.
- **Client Spoofer / Spoofified**: `ComponentUtils.getString()` only used from sign/anvil mixins. Chat/title/item lore probes would resolve for real if those screens call `Component.getString()`. **NF is strictly stronger** on coverage.
- **Debrand / Rebrand / NoFabric / CCB**: no translation protection.

NF is **not** weaker than EP on component coverage. EP's README is outdated. NF is weaker than NPE/NCR only on chat-key privacy, which is a different problem.

---

## Keybind fingerprinting

Mods leak if a server sends `{"translate":"key.meteor-client.open-gui"}` or a `keybind` component and the client echoes a resolved name such as `"Right Shift"` instead of the key or vanilla default.

NF fake-default path protects **vanilla** `key.*` custom bindings from being distinguishable from defaults. It does **not** hide that a **mod keybind exists** unless the translation pipeline returns the raw key / pack value (which NF does for non-vanilla names). If a server-pack defines `key.meteor-client.open-gui`, NF will echo the pack value (vanilla-identical for a pack-using client). That is correct, not a bypass.

Bypasses:

- Asking the player what a control does (social).
- Gameplay: player uses a Meteor bind that vanilla cannot.
- `key.categories.*` as translation keys — covered if they go through `TranslatableContents` from a packet.
- Dynamic bindings registered outside Fabric's registry: still intercepted if wrapped in `KeybindContents`, because interception is by component type, not name prefix. CODE VERIFIED in mixin javadoc.

CS/SF: only when sign/anvil `getString` runs `KeybindContents` through `ComponentUtils.canTranslate` (vanilla/pack language, not live KeyMapping). Narrower.

---

## Custom payload / channel analysis

These are different layers:

1. **Brand string** — `ClientBrandRetriever` / `BrandPayload`
2. **Channel enumeration** — `minecraft:register` list
3. **Channel registration API** — Fabric `PayloadTypeRegistry` / receivers
4. **Outgoing payloads** — C2S custom payload packets
5. **Incoming payloads** — S2C probes, codec success/fail, handler ClassCastException
6. **Fabric networking registration** — still happens in-process even if packets are dropped
7. **Fabric handshake** — register payloads + older login queries
8. **Mod-specific protocols** — voice chat, Via, Essential, Xaero, Meteor

| Layer | NF | CS/SF | Rebrand | NoFabric | EP |
|---|---|---|---|---|---|
| Brand | Opt-in vanilla | Mode vanilla/fabric/custom | Configurable; default path returns `"vanilla"` if networking branding disabled | **Drops the packet** | None |
| Outbound payloads | Vanilla: drop all but brand; Fabric: whitelist | Vanilla: drop non-brand/non-discarded; Modded: prefix allowlist; Custom: optional channel allowlist | Cancels non-`minecraft:(un)register` when ghost-mode gate is false | Cancels all custom C2S | None |
| Register list rewrite | Yes (Fabric mode) | Implicit only by dropping whole register packet in vanilla | Drops register (regex excludes register from allow) | Drops register | None |
| Inbound codec | Yes ≥1.20.5 | No | No | No | No (EP mixin on `PayloadTypeRegistryImpl.register` only **records mods for translation allowlists**) |
| Login query | No | No | Yes (`LoginQueryResponseC2SPacketMixin`) | No | No |

**Fabric still reveals capability in-process.** Filtering packets does not unregister Fabric API receivers. A server that never probes codecs and only looks at register/brand is covered. A server that sends a typed Fabric payload on 1.20.5+ is covered by NF inbound mixins. A server using Velocity login plugin messages is **not** covered by NF; Rebrand attempts this but the enable/disable sense looks inverted.

Legitimate mods: vanilla mode **will** break voice chat, minimaps with serverside integration, ViaFabric extra channels, Essential. Fabric+AUTO whitelist tries to allow mods that registered channels (`WhitelistMode.AUTO`). That is a deliberate compatibility leak: AUTO re-exposes those mods' channels.

Meteor: channels still leaked unless vanilla mode or Meteor not whitelisted. Translation echo mitigated if protection on.

No Chat Reports / NPE: may register their own channels or change signing packets; NF stands down on signing/telemetry only, not their payloads.

---

## Policy model design

**Do not implement yet.** Suggested modes:

### STRICT VANILLA

- Brand `vanilla`
- Drop all non-`minecraft:` (and non-brand) payloads; rewrite register to empty/vanilla-only
- Known-packs: minecraft namespace only
- Translation/keybind: vanilla + server-pack keys only
- Deny Fabric inbound codecs (already)
- Local URL block on
- Pack cache isolation on
- Chat: user-selected SIGN or OFF (do not auto-switch)
- **Breaks** almost every networked mod

### PRIVACY

- Brand remains honest Fabric **or** configurable
- Block known privacy oracles: translation probes, local URLs, pack-cache cross-account, inbound codec throws, telemetry
- Permit whitelisted mod channels (voice, map)
- Known-packs: whitelist
- Default for "I use mods but do not want sign/anvil oracles"

### COMPATIBILITY

- Minimal blocking (current default is closer to this: `spoofAsVanilla=false`, AUTO whitelist, translation on, local URL on, cache isolation on)
- Preserve server-required mod functionality
- Do **not** stand down identity spoofing just because EP is installed; feature-gate overlap only where EP actually implements the same control

### CUSTOM

- Per-mod / per-channel / per-pack triples
- UI already has CUSTOM whitelist; extend to inbound vs outbound, known-packs, shaders

Tradeoff: STRICT VANILLA is the only mode that approximates "brand + channels + known-packs look like a stock client." It cannot hide behavioral anti-cheat, timing, or ClientInformation. PRIVACY without STRICT will still look like Fabric to any channel enumerator.

---

## Known packs

- Protocol introduced **1.20.5** (`KnownPack` / `ServerboundSelectKnownPacks` / `KnownPacksManager`).
- Packet names: configuration-phase select-known-packs (Mojang mappings `ServerboundSelectKnownPacks`).
- Vanilla advertises `minecraft` core packs. Fabric resource-loader injects mod packs as known-pack triples **when `ModPackResourcesUtil` exists (1.21.11+ Fabric API)**.
- NF mixin compiled `>=1.20.5`, effective filtering when hook present.
- Newer fields: KnownPack is `(namespace, id, version)` strings; no extra fields observed in NF's `KnownPack` usage. **NEEDS TESTING** if 26.x added more.
- Client Spoofer only special-cases namespace `fabric` — incomplete vs mods that use their mod id as namespace.
- Not a bug that 1.20.1 has an empty mixin stub.

---

## Resource pack fingerprinting

Attack (public writeups, e.g. Cytooxien / TrackPack style): shared `downloads/` cache means account B hits a pack account A already fetched → faster SUCCESS / no HTTP GET. Server correlates accounts.

NF/EP/CS: subdirectory per **account UUID**.

| Correlation | Server-measurable? | NF status |
|---|---|---|
| Shared disk cache across accounts | Yes (timing / no re-download) | Mitigated if isolation on |
| Same account rejoining | Yes | **Not mitigated** (by design) |
| Per-session empty cache | Would need session dir | Missing |
| HTTP headers / ETag / Java User-Agent | Possible if pack host is attacker-controlled | Unmodified vanilla `HttpUtil` |
| Redirect behavior | Yes | Partial (see URL section) |
| Error strings in disconnect | Unlikely for pack fail | Vanilla exceptions |
| Status ACCEPTED vs DECLINED vs FAILED | Yes | Not spoofed; lang-only still SUCCESS |
| Shared temp files | Possible | UNKNOWN |
| Hash mismatch vs connection refused vs timeout | Yes | NF local-block fails faster than EP's dummy Socket |

Spoofified isolation has a disabled-mod logic bug. CS does not detect sibling isolation (can double-nest if combined with NF; NF checks `original.getParent().equals(cacheDir)` to avoid that).

UUID isolation **does** prevent the simple cross-account cache hit **if enabled and EP is not loaded**. It does **not** prevent correlating one account over time.

---

## Resource pack URL security

NF and EP share the same `InetAddress` classifier. CS/SF have **no** URL filter.

| Check | NF | EP |
|---|---|---|
| `127.0.0.1` / `::1` / `localhost` | Yes (loopback) | Yes |
| `127.0.0.0/8` | Yes (`isLoopbackAddress`) | Yes |
| `10/8`, `172.16/12`, `192.168/16` | Yes (`isSiteLocalAddress`) | Yes |
| `169.254/16`, `fe80::/10` | Yes (`isLinkLocalAddress`) | Yes |
| `::1` | Yes | Yes |
| `fc00::/7` ULA | **NO** | **NO** |
| Deprecated IPv6 site-local `fec0::/10` | Yes (`isSiteLocalAddress` on Inet6) | Yes |
| CGNAT `100.64.0.0/10` | **NO** | **NO** |
| `::ffff:127.0.0.1` | PARTIAL — Java often converts mapped to Inet4 | Same |
| Decimal/hex/octal IPv4 in host | **NO** — `getAllByName` treats as hostname | Same |
| Redirects 300–303, 305, 307 | Per-hop | Per-hop (slightly different 305) |
| Redirect 308 | **NO** | **NO** |
| DNS all A/AAAA at check time | Yes | Yes |
| DNS TOCTOU between check and connect | Residual | Residual |
| Dummy TCP to preserve vanilla timing | Only max-redirect leak | Yes on local block |

**NF is weak on ULA, CGNAT, 308, 1.20.1, and TOCTOU rebinding.** EP is not better on ULA/CGNAT/308; EP is slightly better on TCP-timing camouflage and supports local-block on its (newer) version set.

Validation is **after DNS** (`getAllByName`), **on each redirect hop's URL host**, not on the connected socket's actual peer after connect. A hostname can change records between `getAllByName` and `openConnection`.

---

## Resource pack shader overrides

NF blocks `assets/<namespace>/shaders/**` for non-`minecraft` namespaces that belong to installed mods, unless that namespace is allowed. Vanilla core shaders pass. Mod shaders in the **mod jar** remain (returning null falls through to the mod's own assets).

No competitor implements this.

Bypass **THEORETICAL**: put override shaders under `minecraft` namespace (would affect vanilla pipeline, noisy); or under a namespace that is not detected as mod-owned; or use core-shader JSON that is not under `shaders/`. Core-shader layout changed across versions; NF uses path prefix `shaders/` only.

Legitimate packs that intentionally restyle Sodium/Iris GUIs will be stripped.

---

## Client settings / locale

Vanilla `ServerboundClientInformationPacket` (names vary) includes locale (`en_us`), view distance, chat visibility, chat colors, model customization bitmask, main hand, text-filtering flag, server-listing status, and later particle status.

Fingerprint contribution: locale and view distance are **coarse** (many users share `en_us` / 12). Model bitmask + particles + filtering are a small anonymity set but not a mod detector.

**No inspected project modifies these fields.** Lying about locale breaks command parsing UX and some server-side localization. Do **not** auto-lie. Optional PRIVACY-mode normalization (e.g. force `en_us`) is a P3 with compatibility cost.

---

## Chat signing

| | NF | NCR | NPE |
|---|---|---|---|
| Default | SIGN | Strip / safety-state driven | NO_KEY recommended in README |
| Modes | SIGN, OFF | enableMod + server safety + telemetry toggle | SIGN, NO_SIGN, NO_KEY, ON_DEMAND |
| Profile key suppression | No | `ProfileKeyPairManager.EMPTY_KEY_MANAGER` when signing not allowed | `AccountProfileKeyPairManagerMixin` returns empty when `noKey()` |
| Server-visible | OFF: unsigned chat; key may still exist | Depends on safety level | NO_KEY: no key + unsigned |
| Fingerprint risk of AUTO | NF **removed AUTO** after it fingerprinted | NCR infers from whether server accepted unsigned chat (`UNINTRUSIVE`) | ON_DEMAND prompts — UX fingerprint if unique |

NF OFF is closer to NCR "don't sign" than to NPE NO_KEY. On a server that requires signatures, OFF cannot chat. On a server that optional-signs, OFF is distinguishable from vanilla Java (which signs by default when a key exists).

NCR also disables telemetry via `Minecraft.allowsTelemetry` — same hook NF uses.

---

## Mod-specific detection

Goal: privacy / unnecessary disclosure, not anti-cheat bypass.

| Mod | Vector | Observation | NF mitigation | Remaining |
|---|---|---|---|---|
| Meteor | Sign `getString` raw keys (old mixin); channels; translation keys | Echoed key vs fallback; `meteor:*` payloads | MixinSquared cancel `<26.1`; translation filter; channel filter | Gameplay; 26.1+ other mixins; channels if whitelisted |
| Xaero | Non-standard key names (`gui.xaero_*`); possible channels | Keybind/translation echo | KeybindContents treats **all** names as keybinds | Minimap server mods if channels allowed |
| Simple Voice Chat | `voicechat` payloads + register | Channel list + traffic | Dropped in vanilla mode | Must whitelist to function |
| Fabric API | `fabric:` register, registry sync codec | DecoderException vs ignore | Inbound codec null ≥1.20.5 | Brand `fabric` if not spoofing |
| Essential | Custom channels / login | Payloads | Filtered unless allowed | — |
| ViaFabric / ViaFabricPlus | Extra protocol channels, version handshake | Non-vanilla protocol behavior | Channel filter may break Via | Protocol translation itself is a fingerprint |
| No Chat Reports | Signing behavior, possible extra state | Unsigned chat | Stand-down | Dual-install signing policy |
| Utility clients | Mix of the above + movement | Anti-cheat | Out of scope | Out of scope |

---

## Behavioral fingerprinting

NoFingerprint cannot guarantee a vanilla fingerprint.

**Practical / known:**

- Brand `fabric` vs `vanilla` (default NF is fabric)
- Register channel lists
- Known-pack mod triples on 1.21.11+
- Unsigned chat when mode is OFF
- Local-pack fail latency (NF vs vanilla)
- Fabric codec throw if inbound filter off

**Theoretical:**

- Netty handler presence (not directly visible)
- Packet ordering of filtered vs delayed register
- 308 redirect handling
- JVM HTTP fingerprint of pack downloads
- Lang-only pack with SUCCESS (server cannot see missing textures; anti-cheat might)

**Unknown:** future protocol probes.

Do not market as undetectable, 100% vanilla, or 100% spoofed.

---

## False sense of security

NoFingerprint does **not** guarantee:

- Invisibility to anti-cheat or staff
- Hiding gameplay that only a mod can perform
- Identical packet timing
- Identical error handling on every malformed packet
- Protection when `spoofAsVanilla` is off (the default)
- Protection of brand/channels/translations/cache/URL-block when ExploitPreventer is also installed
- Cross-session unlinkability for the same account
- Locale/ClientInformation anonymity
- Quilt identity
- DNS-rebinding-proof SSRF
- Encrypted account tokens
- That integrity/update checks currently have a real GitHub release trust anchor

---

## Security review

Inherited findings from `SECURITY_AUDIT.md` remain valid. New or restated items from this pass:

### 1. Plaintext session tokens

- File: `accounts/SessionAccount.java` `toJson` / `accounts/AccountManager.java`
- Method: persistence to `config/nofingerprint-accounts.json`
- Severity: **HIGH**
- Problem: access and refresh tokens in plaintext
- Abuse: local malware or shared PC hijacks Microsoft/Minecraft session
- Recommendation: OS keystore or do not persist tokens; warn in UI. Do not remove the manager in this task.

### 2. Account export

- File: `config/NoFingerprintConfigScreen` export dialog (see existing audit)
- Severity: **HIGH**
- Problem: tokens written to a user-chosen path
- Recommendation: encrypt export or exclude tokens by default

### 3. ExploitPreventer stand-down vs `conflicts`

- File: `NoFingerprintConfig.java`, `fabric.mod.json`
- Method: `shouldSpoofBrand`, `shouldSpoofChannels`, …
- Severity: **HIGH** (privacy policy bug, not RCE)
- Problem: dual install disables identity protections EP does not provide
- Abuse: user thinks stacking mods is safer; server still sees Fabric brand/channels
- Recommendation: stand down only overlapping EP features (URL, cache, translation), never brand/channel/known-pack
- **Status (nofingerprint.2): RESOLVED.** Stand-down is URL wrap + translation wrap only. Cache isolation is kept (mixins coexist). `conflicts` removed.

### 4. IPv6 ULA / CGNAT SSRF

- File: `util/LocalAddressUtil.java` `isLocalAddress`
- Severity: **MEDIUM**
- Problem: `fc00::/7` and `100.64.0.0/10` not blocked; server pack URL can hit ULA services
- Abuse: same class as localhost pack SSRF against unique-local IPv6 hosts
- Recommendation: explicit prefix checks after resolving each hop; optionally pin the connected `InetAddress`
- **Status (nofingerprint.2): RESOLVED** in NF (`PrivateAddressClassifier`). **UNCHANGED** in EP; dual-install URL path still uses EP.

### 5. DNS rebinding TOCTOU

- File: `HttpUtilMixin` + `LocalAddressUtil`
- Severity: **MEDIUM**
- Problem: validate-then-connect resolves twice
- Abuse: short-TTL hostname is public at check, private at connect
- Recommendation: connect, then verify **peer address**; refuse if private. Harder with `HttpURLConnection`.
- **Status (nofingerprint.2): PARTIALLY RESOLVED / PARTIALLY MITIGATED.** Not claimed as FIXED. See `RedirectPolicy` and `SECURITY_AUDIT.md`.

### 6. 1.20.1 local URL block disabled

- File: `HttpUtilMixin` else-stub; `MC_VERSION_HAS_BLOCK_LOCAL_URLS`
- Severity: **MEDIUM** on that version
- Problem: classic localhost pack scan works on 1.20.1 builds
- Recommendation: implement a 1.20.1-capable hook or document as unsupported
- **Status (nofingerprint.2): RESOLVED.** 1.20.1/1.20.2 hook `method_15303` / `lambda$downloadTo$0`. `MC_VERSION_HAS_BLOCK_LOCAL_URLS` is true.

### 7. Integrity / update GitHub trust

- File: `config/UpdateChecker.java`, `JarIntegrityChecker.java`
- Severity: **LOW** (fail-closed today) → **HIGH** if a wrong repo is ever shipped
- Problem: advisory SHA-256 vs GitHub Releases; dismissible tamper screen
- Recommendation: signed releases; do not auto-open untrusted URLs beyond GitHub

### 8. Cracked accounts

- File: `accounts/CrackedAccount.java`
- Severity: **INFO** / policy
- Problem: offline-mode usernames in a privacy mod; servers see cracked vs premium differently
- Recommendation: keep isolated from premium token storage

### 9. Recursive cache delete

- File: `protection/ResourcePackGuard` (existing audit)
- Severity: **LOW** if path stays under gameDir
- Recommendation: keep normalization tests

### 10. Netty pipeline injection

- File: `ClientConnectionMixin.nofingerprint$ensurePipelineHandler`
- Severity: **LOW**
- Problem: handler name `nofingerprint_filter`; failure to install falls back to `send()` mixin
- Recommendation: keep both paths; test encoder-only send

### 11. Meteor config read at mixin-plugin time

- File: `MeteorMixinCanceller.readMeteorFixSetting`
- Severity: **INFO**
- Problem: JSON parse of user config very early; malformed JSON defaults to enabled
- Recommendation: acceptable

No command-execution or native downloader beyond `Util.openUri` for GitHub was found.

---

## License review

| Project | License | Incorporate into GPL-3.0 NF? | Recommendation |
|---|---|---|---|
| OpSec | GPL-3.0 | Already the lineage | KEEP CURRENT |
| ExploitPreventer | MIT | Yes, with attribution | REIMPLEMENT CONCEPT (already did for URL/cache/components); MIT code may be ported with copyright notice |
| Client Spoofer | MIT | Yes | REIMPLEMENT CONCEPT only; weaker than NF |
| Spoofified | MIT | Yes | DO NOT USE UI-hiding as "fingerprint" work; cache mixin has a bug |
| haykam Debrand | MIT | Yes | NOT relevant |
| LoganDark debrand | MIT | Yes | Brand-only; skip |
| Rebrand | GPL-3.0-or-later | Compatible | REIMPLEMENT CONCEPT for login-query after fixing their gate logic; do not copy inverted control flow |
| NoFabric | GPL-3.0 | Compatible | DO NOT USE (drops brand) |
| Custom Client Brand | CC0-1.0 | Yes | Unnecessary; NF already spoofs brand |
| No Chat Reports | WTFPL | Yes | REIMPLEMENT CONCEPT for key-manager empty; do not replace NF signing UX blindly |
| No Prying Eyes | MIT | Yes | REIMPLEMENT CONCEPT for NO_KEY |
| Fabric Hider | CC0-1.0 | Yes | Obsolete; F3/log hiding is local |
| IceCream | MIT | Yes | Brand-only |
| LiquidBounce cache mixin | GPL | Compatible if notices kept | Already attributed; KEEP CURRENT |

**DIRECT PORT POSSIBLE** only where license matches **and** the implementation is actually better: NPE key-pair suppression (MIT), NCR empty key manager (WTFPL), EP dummy-Socket timing (MIT) if still desired. Prefer reimplementation with tests.

---

## Better implementations

### ExploitPreventer component pipeline (peer, not strictly stronger)

- Repo: NikOverflow/ExploitPreventer
- Files: `translation/ComponentCodec.java`, `mixin/vanilla/TranslatableContentsMixin.java`, `PacketDecoderMixin.java`
- License: MIT
- Difference: vanilla-only Language swap vs NF's whitelist + pack-echo + alerts
- Why "stronger" in one sense: simpler mark-and-swap, less mode surface
- Why NF is stronger in another: pack-key echo, Meteor, fabric whitelist
- Action: **MERGE APPROACHES** (keep NF policy; steal EP's dummy-Socket timing if tests show NF fail-latency is a practical oracle)

### ExploitPreventer local-block TCP camouflage

- File: `mixin/vanilla/HttpUtilMixin.java` — `new Socket(host,port)` then throw
- Why stronger: closer to vanilla "attempted connect" timing
- Compatibility: the local service **does** see a TCP SYN (may be acceptable; the game server still sees FAILED)
- Action: **RESEARCH MORE** then possibly MERGE

### No Prying Eyes NO_KEY

- File: `mixins/client/AccountProfileKeyPairManagerMixin.java`
- Why stronger: server never receives a chat reporting public key
- Compatibility: servers requiring secure profile / signed chat will reject
- Action: **REIMPLEMENT** as an explicit signing mode, not default

### No Chat Reports empty key manager

- File: `mixins/client/MixinMinecraft.java` `getProfileKeyPairManager`
- Action: **REIMPLEMENT** alongside OFF, or defer entirely to NCR when loaded (already)

### Rebrand login query

- File: `mixins/LoginQueryResponseC2SPacketMixin.java`
- Why different: covers Velocity/plugin login queries NF ignores
- Why not to copy: gate looks inverted; can break proxies
- Action: **RESEARCH MORE**

### Client Spoofer known-packs

- Weaker (namespace `fabric` only). Action: **KEEP CURRENT** (NF's minecraft-namespace allow is correct)

### Client Spoofer / Spoofified translation

- Weaker (sign/anvil only). Action: **KEEP CURRENT**

---

## Missing protections

### P0 — Critical privacy/security

1. **EP coexistence drops identity spoofing** — **RESOLVED** in nofingerprint.2 (`CompatibilityPolicy`).  
   Threat: user stacks EP+NF. Signal: brand `fabric`, full channel list, known-packs. Versions: all. Competitor: none should cause this. Complexity: low (change gates). Compat: low.

2. **IPv6 ULA (and CGNAT) pack SSRF** — **RESOLVED** in NF classifier; dual-install URL path still EP.  
   Threat: pack URL `http://[fd12:…]:port`. Signal: SUCCESS/FAIL/hash. Versions: 1.20.2+. EP same gap. Complexity: medium. Compat: low (might break weird IPv6 LAN packs — skip when server is already local).

3. **1.20.1 local URL block missing** — **RESOLVED** in nofingerprint.2.  
   Threat: classic localhost scan. Versions: 1.20.1 only. Complexity: medium (synthetic HttpUtil method). Compat: low.

### P1 — High-value fingerprint

4. **DNS rebinding / post-connect peer check** — **PARTIALLY RESOLVED** (check-time all records + per-hop; peer not pinned).  
   Versions: HTTP pack download path. Complexity: high. Compat: medium (HTTP stacks).

5. **HTTP 308 (and verify 304/306)** — **RESOLVED** (308 followed; 304/306 not redirects).  
   Complexity: low. Compat: low.

6. **Login-plugin / configuration handshake leftovers**  
   Rebrand attempts this. Complexity: high. Compat: **high** (Velocity).

7. **Inbound Fabric codec filter on remaining version gaps / tests**  
   Already present ≥1.20.5; needs protocol tests. Complexity: tests more than code.

8. **Chat NO_KEY mode** (optional, not default)  
   NPE. Complexity: medium. Compat: high on enforcing servers.

### P2 — Useful hardening

9. **Policy engine (STRICT/PRIVACY/COMPAT/CUSTOM)** — UX + less foot-guns  
10. **Per-session pack cache** — extra unlinkability; more downloads  
11. **Document that pack SUCCESS + lang-only is intentional**  
12. **Explicit address literals** (decimal IPv4)  
13. **Fix `conflicts` vs runtime EP policy** so metadata matches behavior  

### P3 — Experimental / low-value

14. Locale/`ClientInformation` normalization  
15. Quilt brand  
16. HTTP header normalization (likely identical to vanilla already)  
17. Timing padding on local-URL fail  
18. F3/log hiding (Fabric Hider) — not server-visible  

---

## Inherited OpSec functionality classification

| Item | Class |
|---|---|
| Account manager + cracked accounts | KEEP (product); REWRITE storage |
| UpdateChecker / JarIntegrityChecker | KEEP; REWRITE trust anchor when GitHub releases exist |
| Config migration `opsec.json` → `nofingerprint.json` | KEEP |
| Meteor fix | KEEP; VERSION LIMITED ≥26.1 |
| TrackPackDetector alerts | KEEP (detection UX, not a hide) |
| AUTO signing (already removed) | KEEP removed |
| WhitelistMode.OFF user-selectable (migrated away) | KEEP migrated |
| Title-screen update nag | KEEP or REMOVE LATER (privacy-irrelevant) |
| `minecraft:mco` drop in fabric mode | UNKNOWN — document why; may be leftover |
| Dual pipeline + send() filter | KEEP until tests prove one is enough |

---

## Source coverage

Code-verified from clones on 2026-09-19:

NoFingerprint, OpSec, ExploitPreventer, ClientSpoofer, Spoofified, haykam821/Debrand, LoganDark/debrand, kaylendog/rebrand, qtchaos/NoFabric, MrKinau/CustomClientBrandMod, Aizistral-Studios/No-Chat-Reports (`26.2-Unified`), Daxanius/NoPryingEyes, trolltaylor/fabric-hider, Bawnorton/IceCream.

Not fully cloned: LiquidBounce (only the already-attributed cache idea).  
No closed-source "Client Spoofer" variant was treated as verified.  
Quilt-specific mods: none found.
