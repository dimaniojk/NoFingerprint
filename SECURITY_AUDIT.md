# NoFingerprint Security Audit

Initial review performed during the OpSec → NoFingerprint rename. This is not a full adversarial assessment. Protection logic was not redesigned in this pass.

## Account credentials

- `accounts/SessionAccount.java` stores Microsoft/Minecraft **access tokens** and optional **refresh tokens**.
- `accounts/AccountManager.java` persists them to `config/nofingerprint-accounts.json` as **plaintext JSON** (`accessToken`, `refreshToken`, username, UUID).
- Import/export (`NoFingerprintConfigScreen.openExportDialog` / import) writes the same plaintext JSON to a user-chosen path via TinyFileDialogs.
- `AccountManager.captureOriginalAccount()` copies the currently running launcher session token into memory so logout can restore it.
- `SessionAccount.login()` uses mixins on `Minecraft` (`MinecraftAccessor`) to replace `User`, `ProfileKeyPairManager`, `UserApiService`, and `PlayerSocialManager` at runtime.
- Token refresh talks to Microsoft/Xbox/Minecraft auth endpoints listed in `config/NoFingerprintConstants.AuthUrls`, trying several public Azure client IDs from `AzureClientIds`.
- First-run migration copies upstream `config/opsec-accounts.json` to `config/nofingerprint-accounts.json` if the new file is missing (`config/ConfigFiles.java`). That preserves tokens for OpSec users; it also means leftover OpSec account files remain on disk unless the user deletes them.

**Risk:** anyone with filesystem access to `.minecraft/config` can read session tokens and hijack accounts. Export dialogs can write tokens outside the game directory.

## Network requests

- `config/UpdateChecker.java` — GitHub Releases API `https://api.github.com/repos/dimaniojk/NoFingerprint/releases/latest`. User-Agent `NoFingerprint-Mod/<version>`. Failure is swallowed; a missing repo currently means no update prompt.
- `config/JarIntegrityChecker.java` — GitHub Releases API `.../releases/tags/V<version>`, then compares local JAR SHA-256 to the asset `digest` field. 404 (no release yet) skips the check. Download buttons open `UpdateChecker.getReleaseUrl()`.
- `accounts/SessionAccount.java` — HTTPS to Minecraft profile, Microsoft token, Xbox, XSTS, and Minecraft login-with-Xbox endpoints. Access tokens are sent as `Authorization: Bearer`.
- `mixin/client/HttpUtilMixin.java` — intercepts resource-pack HTTP downloads to refuse local/private redirect targets when enabled.
- `mixin/client/YggdrasilUserApiServiceMixin.java` — telemetry-related user API service hook.
- `mixin/client/MinecraftMixin.java` — can disable telemetry sending.

No other outbound HTTP was found in this pass. There is no crash-reporting endpoint and no custom telemetry to a NoFingerprint server.

Until this fork publishes GitHub releases, integrity/update checks are expected to no-op. After releases exist, a wrong repo URL would either miss updates or, worse, point users at someone else's artifacts.

## Local file access

- Settings: `config/nofingerprint.json` via `NoFingerprintConfig` (atomic temp-file replace).
- Accounts: `config/nofingerprint-accounts.json`.
- Language strings: classpath-only `assets/nofingerprint/nofingerprintlang/` — deliberately **not** vanilla `lang/`, so server resource packs cannot override UI text (`lang/NoFingerprintLang.java`).
- Resource pack cache isolation uses vanilla `downloads/` (and legacy `server-resource-packs/`) with **per-account UUID subdirectories**. Isolation is path layout only; it does not encrypt cached packs (`DownloadQueueMixin`, `LegacyDownloadedPackSourceMixin`).
- `protection/ResourcePackGuard.clearAllCaches()` recursively deletes files under `downloads/` and `server-resource-packs/` inside the game directory, then tries to reset in-memory download state. This is user-triggered from the settings screen.
- Mixin canceller reads config at class-init time (`mixin/MeteorMixinCanceller.java`) before the rest of the mod loads.

## Resource packs

- URL allow/block: `util/PrivateAddressClassifier.java` + `util/RedirectPolicy.java` + `HttpUtilMixin` + `detection/TrackPackDetector.java`. Local/private classification is explicit CIDR (including CGNAT and IPv6 ULA), not `InetAddress.isSiteLocalAddress()`.
- Forced-pack bypass wraps pack resources so language files can stay loaded while textures/shaders are stripped (`protection/LangOnlyPackResources.java`, `PackStripHandler.java`, `PackStripOverlay.java`).
- Shader override stripping for non-whitelisted mods: `protection/ShaderStripTracker.java` and pack mixins.
- Cache isolation is UUID-based, not username-based. Switching accounts without isolation enabled still shares the vanilla cache.

Do not change the UUID subdirectory scheme without a dedicated privacy review; that is the cross-account fingerprint defense.

## Server-controlled input

- Translation/keybind probes: `protection/TranslationProtectionHandler.java`, `TranslatableContentsMixin`, `KeybindContentsMixin`, `ComponentSerializationMixin`, packet mixins (`PacketDecoderMixin`, `PacketProcessorMixin`, `PacketUtilsMixin`, `ClientPacketListenerMixin`).
- Network channel filtering: `protection/ClientSpoofer.java`, `ChannelRegistrationMixin`, `PayloadTypeRegistryImplMixin`, `AbstractChanneledNetworkAddonMixin`.
- Known-pack filtering: `KnownPacksManagerMixin` (MC 1.20.5+ / meaningful on 1.21.11+ Fabric).
- Chat signing strip/control: `ServerboundChatPacketMixin`, `ClientPacketListenerMixin`.
- Resource-pack push handling: `ClientCommonPacketListenerImplMixin`, `DownloadedPackSourceMixin`.

Inbound packets are treated as hostile when spoofing/protection is on. The private language table exists specifically because vanilla `Language` is server-influenceable.

## Mixins / invasive hooks

The client mixin set is intentionally broad. High-impact targets include:

| Area | Classes |
|------|---------|
| Session swap | `MinecraftAccessor`, `MinecraftMixin` |
| Brand | `ClientBrandRetrieverMixin` |
| Packets / codecs | `PacketDecoderMixin`, `PacketProcessorMixin`, `PacketUtilsMixin`, `ComponentSerializationMixin` |
| Translations / keys | `TranslatableContentsMixin`, `KeybindContentsMixin`, `ClientLanguageMixin` |
| HTTP / packs | `HttpUtilMixin`, `DownloadQueueMixin`, `DownloadedPackSourceMixin` |
| Chat | `ServerboundChatPacketMixin` |
| Telemetry | `YggdrasilUserApiServiceMixin`, `MinecraftMixin` |
| UI | `JoinMultiplayerScreenMixin`, `TitleScreenMixin`, `TabNavigationBarMixin` |
| Other mods | `MeteorMixinCanceller` (MixinSquared) |

Mixin unique members were renamed `opsec$…` → `nofingerprint$…`. Accessor names are internal to this mod.

## Potential vulnerabilities

1. **Plaintext session tokens on disk** — highest practical risk for users of the account manager.
2. **Account export** writes tokens to an arbitrary path chosen in a native file dialog.
3. **Update / integrity trust** currently depends on GitHub `dimaniojk/NoFingerprint`. That repository did not exist at fork time; checks fail closed (skip), which is safe but means unofficial builds will not warn.
4. **Integrity check is advisory** — mismatch shows `TamperWarningScreen` and can be permanently dismissed (`tamperWarningDismissed` in settings).
5. **Azure client ID rotation** in token refresh may be rejected by Microsoft or may not match the client that issued the refresh token.
6. **Recursive cache delete** is scoped to game-dir cache folders, but a path-resolution bug there would be destructive. The current code normalizes against `Minecraft.gameDirectory`.
7. **Fabric `conflicts` with ExploitPreventer** existed in `fabric.mod.json` at fork time while runtime also implemented a blanket compatibility stand-down. **RESOLVED in 1.1.7.1-nofingerprint.2:** `conflicts` was removed; stand-down is limited to overlapping HTTP and translation hooks (`CompatibilityPolicy`).
8. **Mod Menu icon** — the upstream OpSec “OP” mark was replaced in 1.1.7.1-nofingerprint.2-beta.1 with an original geometric asset. It is not a finished brand logo.

No command execution / `ProcessBuilder` / native payload downloaders were found beyond opening a release URL with `Util.getPlatform().openUri`.

## Recommended future changes

1. Commission designed NoFingerprint branding if the current geometric icon is too plain for store pages.
2. Encrypt or OS-keychain-protect `nofingerprint-accounts.json`, or stop persisting access tokens.
3. Publish signed GitHub releases so `JarIntegrityChecker` and `UpdateChecker` have a real trust anchor. Pin the expected repo and consider minisign/GPG for artifacts.
4. Revisit whether the account manager belongs in a privacy mod; it expands the secret surface significantly.
5. Add a one-time UI notice when migrating from `opsec.json` / `opsec-accounts.json`.
6. After official hosting exists, remove any remaining temptation to send users to aurickk/OpSec for "updates".
7. Decide whether ExploitPreventer should `conflicts` or coexist; the metadata and runtime behavior currently disagree in tone. **Done in nofingerprint.2:** coexist, with a documented capability matrix.
8. Keep Stonecutter multi-version support; do not collapse mixins into a single-version rewrite before a behavior audit.

## Hardening Phase 1 (1.1.7.1-nofingerprint.2)

This pass changed protection logic. Historical findings above were marked rather than deleted.

### ExploitPreventer coexistence — RESOLVED

Blanket `!EXPLOIT_PREVENTER_LOADED` gates were replaced with `CompatibilityPolicy`. Kept under NoFingerprint when EP is present: brand spoof, channel filter, known-pack filter, pack-strip, shader strip, pack-cache isolation. Stood down: local/private pack URL `HttpUtil` wrap (same `getInputStream` WrapOperation) and translation/keybind component wraps (same `Language.getOrDefault` path). Dual-install URL blocking therefore uses EP's weaker classifier (no CGNAT/ULA/308). Settings UI greys only those two controls and explains the overlap.

### Local/private address classification — RESOLVED (NF path)

`PrivateAddressClassifier` no longer relies on `InetAddress.isSiteLocalAddress()`. Blocked IPv4: `0.0.0.0/8`, `10/8`, `100.64.0.0/10` (CGNAT), `127/8`, `169.254/16`, `172.16.0.0/12`, `192.168.0.0/16`, `224.0.0.0/4` multicast, `240.0.0.0/4` reserved. Blocked IPv6: `::`, `::1`, `fc00::/7` ULA, `fe80::/10` link-local, `ff00::/8` multicast. IPv4-mapped (`::ffff:x.x.x.x`) and deprecated IPv4-compatible IPv6 are unwrapped and classified as IPv4. A hostname is unsafe if **any** `getAllByName` address is blocked; IPv6 answers are not ignored.

### HTTP redirects — RESOLVED (308); DNS TOCTOU — PARTIALLY MITIGATED

Followed: 300, 301, 302, 303, 305, 307, **308**. Not followed: 304, 306, non-HTTP(S) Location. Each hop is classified before `openConnection`. Depth cap 20; repeated hop URLs throw. LAN skip still applies when the connected game server itself is local/private.

DNS TOCTOU is **PARTIALLY MITIGATED**, not FIXED. Reason: Minecraft resource-pack downloads use JDK `HttpURLConnection`. The mixin can resolve and classify the host before `openConnection`, but `HttpURLConnection` may resolve the same hostname again at connect. Pinning the validated `InetAddress` onto the TCP connection would require replacing that stack. Do **not** claim DNS-rebinding protection.

### Minecraft 1.20.1 local URL block — RESOLVED

`HttpUtil.downloadFile` does not exist on 1.20.1. The 1.20.1/1.20.2 path is `HttpUtil.method_15303` (Mojmap/intermediary for the `downloadTo` lambda), which does call `HttpURLConnection.getInputStream()`. The mixin targets that method instead of stubbing. `MC_VERSION_HAS_BLOCK_LOCAL_URLS` is true on all supported versions.
