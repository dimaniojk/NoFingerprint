# NoFingerprint Implementation Roadmap

Research date: **2026-09-19**.  
This began as a **plan only**. Hardening Phase 1 shipped in **1.1.7.1-nofingerprint.2** (see status notes below). Remaining phases are still plan-only.

Priorities: P0 critical privacy/security, P1 high-value fingerprint, P2 hardening, P3 experimental.

Referenced implementations are for design, not copy-paste unless the license row in `COMPETITOR_AUDIT.md` says DIRECT PORT POSSIBLE and notices are preserved.

---

## Phase 1 - correctness fixes

Fix policy bugs and version holes that make current protections lie.

**Phase 1 implementation status (nofingerprint.2):** 1.1 and 1.2 are done. 1.3 and 1.4 were **not** part of this pass.

### 1.1 Stop ExploitPreventer stand-down from disabling identity spoofing

- Priority: **P0**
- **Status: RESOLVED** (`config/CompatibilityPolicy.java`). Brand, channels, known-packs, pack-strip, shaders, and cache isolation stay under NF. HTTP URL wrap and translation wrap stand down because dual Mixins would conflict. `fabric.mod.json` `conflicts` removed. Unit tests: `CompatibilityPolicyTest`.
- Problem: `NoFingerprintConfig.shouldSpoofBrand/Channels/KnownPacks` and translation/cache/URL/pack-strip all return false when `exploitpreventer` is loaded. EP does not spoof brand, filter channels, or filter known-packs. Dual install advertises Fabric honestly while the user believes they stacked defenses. `fabric.mod.json` `conflicts` only warns.
- Files likely affected: `config/NoFingerprintConfig.java`, `fabric.mod.json`, config screen greying logic, `COMPETITOR_AUDIT.md` once implemented
- Minecraft versions: all
- Expected behavior: stand down **only** features EP actually implements (local HTTP block, pack-cache isolation, translation/keybind filter). Keep brand/channel/known-pack/shader/pack-strip under NF control. Decide whether `conflicts` stays, becomes `breaks`, or is removed to match coexistence.
- Regression tests: unit-test the gate methods with a stubbed "EP loaded" flag; document the matrix in a comment next to `EXPLOIT_PREVENTER_LOADED`
- Compatibility risks: two translation filters at once if both remain on — keep translation stand-down **or** make NF skip `TranslatableContentsMixin` when EP's module is enabled, but **never** skip brand
- Source/reference: EP `Utils.isLocalAddress`, `DownloadQueueMixin`, `ComponentCodec` (MIT, reimplement/keep current)

Note vs original expected behavior: cache isolation was listed as an EP stand-down candidate; dual `ModifyExpressionValue` on the cache path is safe (NF skips if already reparented), so NF **keeps** isolation rather than standing down.

### 1.2 Restore or officially drop 1.20.1 local-URL blocking

- Priority: **P0** (for the 1.20.1 artifact) / document if dropped
- **Status: RESOLVED.** `HttpUtilMixin` hooks `method_15303` / `lambda$downloadTo$0` on 1.20.1 and 1.20.2 (verified: both call `HttpURLConnection.getInputStream`). `MC_VERSION_HAS_BLOCK_LOCAL_URLS = true`. Classifier is `PrivateAddressClassifier` (CIDR, not `isSiteLocalAddress`). Redirect tests use a local `HttpServer` (302/308 → loopback).
- Problem: `HttpUtilMixin` is a stub on 1.20.1; `MC_VERSION_HAS_BLOCK_LOCAL_URLS = false`. Localhost pack SSRF works.
- Files: `mixin/client/HttpUtilMixin.java`, `NoFingerprintConfig.java`, README/version notes
- Versions: 1.20.1 (1.20.2 already hooked via `method_15303`)
- Expected: same `LocalAddressUtil` policy as later versions, or a printed "unsupported on 1.20.1" in the config UI
- Tests: fake HTTP server on 127.0.0.1; pack push; expect no GET body success
- Compat: LAN servers are already skipped when `serverAddress` is local
- Reference: EP `HttpUtilMixin` (newer MC only)

### 1.3 Signing mode documentation vs leftover AUTO language

- Priority: **P2**
- Problem: `SigningMode` is SIGN/OFF only; some UI/docs/user memory still expect AUTO. Behavior is correct; messaging must match.
- Files: `SpoofSettings.java` (already documented), README, lang strings
- Expected: no AUTO control; explain why it was removed (fingerprint)
- Tests: none beyond config round-trip
- Compat: none

### 1.4 `minecraft:mco` drop audit

- Priority: **P2**
- Problem: fabric mode always cancels `minecraft:mco`. Comment says "match stock Fabric." Unverified across 1.20–26.2.
- Files: `ClientConnectionMixin.java`
- Expected: keep if vanilla Fabric clients never send it; remove if they do
- Tests: packet recorder on stock Fabric vs NF fabric mode
- Compat: Realms-related only; low for third-party servers

---

## Phase 2 - fingerprint protection improvements

### 2.1 Inbound Fabric codec tests (already implemented ≥1.20.5)

- Priority: **P1**
- Problem: `PayloadTypeRegistryImplMixin` + `AbstractChanneledNetworkAddonMixin` are the right design but untested live.
- Files: those mixins, `protection/ClientSpoofer.java`, new test harness
- Versions: 1.20.5+
- Expected: malformed Fabric S2C does not produce a unique disconnect vs vanilla
- Tests: custom payload probe server (see Test strategy)
- Compat: none if already matching vanilla ignore
- Reference: none better than current NF

### 2.2 Known-pack filtering tests (1.21.11+)

- Priority: **P1**
- Problem: mixin exists; Fabric hook probe exists; not proven against a server that offers mod triples
- Files: `KnownPacksManagerMixin.java`, `ModRegistry.java`
- Expected: vanilla mode → only `minecraft` namespace; fabric+whitelist → allowed triples only; `<1.20.5` no packet
- Tests: mock configuration handshake
- Compat: mods that **require** known-pack sync will break in STRICT VANILLA — expected
- Reference: CS `KnownPacksManagerMixin` is weaker (`fabric` namespace only) — do not port

### 2.3 Optional NO_KEY chat mode

- Priority: **P1** (optional feature, default remains SIGN)
- Problem: OFF strips signatures but may still send a profile key. NPE `NO_KEY` suppresses the key pair.
- Files: `SpoofSettings.SigningMode`, `ServerboundChatPacketMixin`, `ClientPacketListenerMixin`, new mixin on `AccountProfileKeyPairManager` or `Minecraft.getProfileKeyPairManager`
- Versions: 1.19+
- Expected: third mode does not send `ServerboundChatSessionUpdate` key material; SIGN unchanged; stand-down if NCR/NPE loaded
- Tests: packet recorder on a server that logs session updates
- Compat: **high** — enforcing-secure-chat servers kick
- Reference: NPE `AccountProfileKeyPairManagerMixin` (MIT, REIMPLEMENT); NCR `MixinMinecraft.getProfileKeyPairManager` (WTFPL)

### 2.4 Login-query / proxy handshake research

- Priority: **P1** research, implement only after Velocity/Fabric login-query map
- Problem: NF ignores login plugin messages. Rebrand nulls `LoginQueryResponseC2SPacket` but the enable gate disagrees with comments and can break proxies.
- Files: new mixin only after a written probe table
- Expected: do not brick Velocity; do not claim vanilla if Fabric still answers Fabric queries
- Tests: Velocity + Fabric server + vanilla client baseline
- Compat: **very high**
- Reference: Rebrand `LoginQueryResponseC2SPacketMixin` — RESEARCH MORE, do not copy

### 2.5 Policy engine (STRICT VANILLA / PRIVACY / COMPATIBILITY / CUSTOM)

- Priority: **P2**
- Problem: `spoofAsVanilla` plus a separate whitelist is easy to misconfigure (vanilla brand + mod channels).
- Files: `SpoofSettings.java`, config screen, `ClientSpoofer.java`, `ModRegistry.java`
- Expected:
  - STRICT VANILLA: brand vanilla, no mod channels, minecraft known-packs, translation vanilla+pack, local URL on, isolation on
  - PRIVACY: keep Fabric brand, block oracles (translation, SSRF, cache, inbound codec)
  - COMPATIBILITY: current defaults
  - CUSTOM: per-mod channel/translation/known-pack toggles (mostly exists)
- Tests: config fixtures per mode vs expected filter decisions
- Compat: STRICT VANILLA will break voice/minimap/Via extras — must be explicit in UI
- Reference: CS `SpoofMode` (too coarse)

---

## Phase 3 - resource pack security

### 3.1 Block IPv6 ULA and CGNAT

- Priority: **P0**
- **Status: RESOLVED** in nofingerprint.2 (`PrivateAddressClassifier`). Also covers `0.0.0.0/8`, multicast `224.0.0.0/4` and `ff00::/8`, reserved `240.0.0.0/4`, link-local, IPv4-mapped and IPv4-compatible IPv6. Tests: `PrivateAddressClassifierTest`.
- Problem: `LocalAddressUtil` uses `InetAddress.isSiteLocalAddress()` which is **not** `fc00::/7`. `100.64.0.0/10` is also unblocked.
- Files: `util/LocalAddressUtil.java`, tests
- Versions: 1.20.2+ (wherever HttpUtil mixin is live)
- Expected: `fd00::/8` (ULA), `fc00::/7`, `100.64.0.0/10` treated as local; still skip when the game server itself is local
- Tests: unit tests with literal addresses (no DNS); mapped IPv6; `::1`
- Compat: unusual IPv6 LAN pack hosts — same as current RFC1918 skip
- Reference: EP `Utils.isLocalAddress` has the **same bug** — do not copy

### 3.2 Redirect 308 and peer-address verification

- Priority: **P1**
- **Status: PARTIALLY RESOLVED.** 308 is followed and every hop is classified before `openConnection`. DNS TOCTOU is **PARTIALLY MITIGATED**, not FIXED: Java `HttpURLConnection` may resolve the hostname again at connect. Pinning the connected `InetAddress` would require replacing the HTTP stack; that was out of scope for Phase 1.
- Problem: hops 300–303/305/307 only; DNS TOCTOU between `getAllByName` and connect
- Files: `mixin/client/HttpUtilMixin.java`, `LocalAddressUtil.java`
- Expected: treat 308 like 307; after connect, if the connected `InetAddress` is local, abort (hard with `HttpURLConnection` — may need to keep manual hop loop and resolve once per hop, connect via that `InetAddress`)
- Tests: redirect test server: public → 127.0.0.1; public → `[fd00::1]`; 308 hop
- Compat: broken HTTPS middleboxes; keep cross-protocol reject
- Reference: NF HttpUtil is already more careful than EP on 305/Authenticator; MERGE timing research from EP dummy Socket

### 3.3 Dummy-Socket timing (research)

- Priority: **P2**
- Problem: NF local-block fails without TCP; vanilla/EP SYN then fail. Server-visible via FAILED latency; local service via SYN.
- Files: `HttpUtilMixin.java`
- Expected: decide with measurements. If FAILED RTT is used in the wild, mimic vanilla. If local SYN is the exploit, **do not** add dummy Socket.
- Tests: timing harness
- Reference: EP `HttpUtilMixin` Socket-then-throw (MIT)

### 3.4 Literal / weird host parsing

- Priority: **P2**
- **Status: DOCUMENTED / N/A for extra parsers** in nofingerprint.2. `JavaAddressSyntaxTest` shows this JDK keeps `127.1`, `2130706433`, and `0x7f000001` as hostnames. No custom parser was added. Bracket IPv6 and `::ffff:` mapped form are classified.
- Problem: decimal IPv4, unusual IPv6 forms, IPv4-mapped
- Files: `LocalAddressUtil.java`
- Expected: reject if **any** interpreted form is local
- Tests: table-driven hosts
- Compat: low

### 3.5 Per-session cache (optional)

- Priority: **P2**
- Problem: UUID dirs stop cross-account hits, not long-term same-account hits
- Files: `DownloadQueueMixin.java`, `LegacyDownloadedPackSourceMixin.java`, config
- Expected: optional session UUID subdirectory; warn about re-download bandwidth
- Tests: two joins, same account, isolation on → second join still HTTP GETs
- Compat: slower joins, more disk
- Reference: none

### 3.6 Shader strip documentation and namespace tests

- Priority: **P2**
- Problem: `LangOnlyPackResources` strips `assets/<ns>/shaders/*` for mod namespaces. Core-shader layouts differ by version.
- Files: `LangOnlyPackResources.java`, `ModRegistry.java`, `ShaderStripTracker.java`
- Expected: vanilla `minecraft` shaders never stripped; document Iris/Sodium pack breakage
- Tests: fake pack with `minecraft/shaders` vs `sodium/shaders`
- Compat: medium for shader packs

---

## Phase 4 - networking policy engine

Depends on 2.5. Implementation details:

### 4.1 Split inbound vs outbound allowlists

- Priority: **P2**
- Problem: allowing a channel for voice C2S also enables inbound codec (presence). Some mods only need one direction.
- Files: `ClientSpoofer.java`, `ModRegistry.java`, `PayloadTypeRegistryImplMixin.java`, `ClientConnectionMixin.java`
- Expected: CUSTOM can allow C2S without installing S2C codec (or the reverse)
- Tests: probe server
- Compat: mods that require both directions

### 4.2 AUTO whitelist leak warning

- Priority: **P2**
- **Status: PARTIALLY RESOLVED** in nofingerprint.2 — settings UI shows: "Mods using allowed network channels can reveal their presence to the server." `/nofingerprint info` was not changed.
- Problem: AUTO re-exposes every mod that registered a channel — correct for compatibility, silent as privacy
- Files: config UI, `/nofingerprint info`
- Expected: show "these channels are visible to the server"
- Tests: none
- Compat: none

### 4.3 Quilt / extra brands

- Priority: **P3**
- Problem: no Quilt identity
- Files: `SpoofSettings` brands
- Expected: only if someone actually runs Quilt with this jar (they do not today)
- Compat: N/A

---

## Phase 5 - privacy hardening

### 5.1 Account token storage

- Priority: **P0** for security, not fingerprinting
- Problem: plaintext `accessToken`/`refreshToken` in `nofingerprint-accounts.json`; export writes tokens
- Files: `accounts/SessionAccount.java`, `accounts/AccountManager.java`, export UI
- Expected: OS keystore or encrypted file; export opt-in "include secrets"
- Tests: round-trip login without leftover plaintext
- Compat: migration from current JSON
- Reference: existing `SECURITY_AUDIT.md`

### 5.2 Update/integrity trust anchor

- Priority: **P2** until GitHub releases exist; **P0** if a URL is ever wrong
- Files: `UpdateChecker.java`, `JarIntegrityChecker.java`
- Expected: fail closed; pin repo; signed artifacts
- Tests: mock 404 vs digest mismatch
- Compat: none

### 5.3 ClientInformation / locale (do not enable by default)

- Priority: **P3**
- Problem: locale is a weak fingerprint
- Expected: if ever added, PRIVACY-only opt-in force `en_us` with a compatibility warning
- Tests: packet dump
- Compat: **high** for non-English servers/commands
- Reference: no competitor does this — RESEARCH MORE

### 5.4 Telemetry

- Priority: keep current
- Problem: already blocked; stands down for NCR/NPE
- Action: **KEEP CURRENT**

---

## Phase 6 - compatibility

### 6.1 Feature-level stand-down matrix

- Priority: **P0** (with 1.1)
- Expected table:

| Foreign mod | NF stands down | NF keeps |
|---|---|---|
| ExploitPreventer | translation **or** URL **or** cache if EP module on | brand, channels, known-packs, shaders, pack-strip |
| No Chat Reports | signing, telemetry | everything else |
| No Prying Eyes | signing, telemetry | everything else |
| Meteor | cancel broken sign mixin `<26.1` | rest |

### 6.2 Voice / Xaero / Via / Essential presets

- Priority: **P2**
- Problem: STRICT VANILLA breaks them; CUSTOM is manual
- Expected: documented presets, not silent AUTO
- Compat: the whole point

### 6.3 Do not add anti-cheat bypasses

- Priority: standing rule
- No movement, reach, or "look vanilla while cheating" work

---

## Phase 7 - tests

See **Test strategy** below. Phase 7 is standing work that starts as soon as Phase 1 lands.

---

## Test strategy

Do not build a full fake Minecraft in this research pass. Design:

### Unit (no Minecraft)

- `LocalAddressUtil` table: loopback, RFC1918, link-local, ULA, CGNAT, mapped IPv6, `null`, unresolvable host
- Gate methods in `NoFingerprintConfig` with injectable "mod loaded" flags (today they are `static final` from `FabricLoader` — make test seams)
- `ModRegistry` whitelist decisions with fixtures

### Fake resource-pack HTTP server

- Endpoints: 200 zip, 301→127.0.0.1, 308→private, 305 proxy, ULA literal, slow vs cached
- Assert: blocked hosts never return 200 body to the game; status packet FAILED vs SUCCESS

### Redirect / DNS test server

- Two A records / hosts file flip between checks (rebinding)
- Expected **future** behavior: blocked after peer validation

### Translation probe server

- Send a sign with `{"translate":"key.meteor-client.open-gui"}` and open-editor echo
- Expected: NF does not echo the live key name; pack-defined keys do echo
- Also: chat-only translate (no echo) — no protocol difference required

### Custom payload probe server

- Send malformed `fabric:registry/sync`
- Expected: vanilla-like ignore when STRICT/PRIVACY inbound filter on
- Send `minecraft:brand` request by reading C2S brand
- Expected: depends on policy mode

### Known-pack mock

- Offer `minecraft` + `fabric-like` triples
- Expected: filtered list on 1.21.11+; no packet on 1.20.1

### Packet recorder

- Proxy or Fabric integration client that logs C2S custom payload ids, register lists, chat signatures, client information
- Compare: vanilla jar vs NF STRICT vs NF default

### Fabric integration tests

- Loom `runClient` is insufficiently automatable; prefer a headless game-test or a dedicated mock server (Paper plugin / Fabric server mod) that records joins

---

## Runtime smoke-test plan

The rebrand verified remapped JAR metadata only. **Do not claim runtime success until these are actually run.**

For **1.20.1**, **1.21.11**, and **26.2**:

1. Launch Fabric client with the matching NF jar + Fabric API.
2. Confirm loader lists `nofingerprint`.
3. Confirm log lines that mixins applied (`ClientBrandRetriever mixin active`, pipeline filter on ≥1.20.2, no mixin apply errors).
4. Open Mod Menu → NoFingerprint → config saves.
5. Join a **vanilla** server (or local vanilla dedicated): chat works; brand dump if the server logs it.
6. Join a **Fabric** server with a benign networking mod: COMPATIBILITY/default still functions; STRICT VANILLA (once it exists) does not.
7. Resource pack: push `http://127.0.0.1:<closed>/pack.zip` — expect block on **1.20.1, 1.21.11, and 26.2** (Phase 1 restored the 1.20.1 hook). Still **NOT RUNTIME TESTED** in a live client.
8. Translation probe: operator sign with a fake mod key; screenshot/log NF alert; confirm echoed text is key/fallback not a mod string.
9. Network channel: dump register list from the server console.

Until that happens, status is **NOT RUNTIME TESTED**.

---

## Proposed implementation order

Exact order for future work:

1. ~~EP coexistence gates (1.1 / 6.1)~~ **done in nofingerprint.2**
2. ~~`LocalAddressUtil` ULA + CGNAT (3.1)~~ **done in nofingerprint.2**
3. ~~1.20.1 URL block or honest UI (1.2)~~ **done in nofingerprint.2**
4. ~~Redirect 308 + address tests (3.2 / 3.4)~~ **308 done; DNS TOCTOU remaining as PARTIALLY MITIGATED**
5. Live client verification of 1.20.1 `method_15303` mixin (pack push to localhost) — still missing
6. Inbound codec + known-pack live tests (2.1 / 2.2)
7. Token storage (5.1)
8. Policy engine UX (2.5 / 4.x)
9. Optional NO_KEY (2.3)
10. Login-query research (2.4)
11. Dummy-Socket decision (3.3)
12. Per-session cache (3.5)
13. Integrity signing (5.2)
14. P3 locale / Quilt / header myths  

---

## Task template (for each future PR)

Every implementation PR should list: Priority, Problem, Files, Versions, Expected behavior, Regression tests, Compatibility risks, Source/reference, and **must not** claim undetectability.
