# NoFingerprint

A client-side Fabric privacy mod designed to reduce Minecraft client fingerprinting and tracking vectors.

NoFingerprint is an independently maintained fork of [OpSec](https://github.com/aurickk/OpSec) by aurickk. It keeps the upstream protection behavior and multi-version Fabric build, with a separate project identity.

> [!IMPORTANT]
> Only download NoFingerprint from official NoFingerprint sources. Third-party reuploads, Discord attachments, and random file hosts are not official and may be malicious.
>
> Intended official source:
> - **[GitHub Releases](https://github.com/dimaniojk/NoFingerprint/releases)**
>
> Upstream OpSec pages (Modrinth, CurseForge, and aurickk/OpSec releases) distribute **OpSec**, not NoFingerprint.

The bundled Fabric/Mod Menu icon is still the upstream OpSec "OP" mark. It does not contain the word OpSec, but it is temporary branding and should be replaced.

## Features

- **Spoof as Vanilla** — Set the client brand to vanilla and block mod detections
- **Channel Spoofing** — Conditionally block mod network channels to prevent detection
- **Known-Pack Filtering** — Conditionally strip built-in pack identifiers from the configuration handshake
- **Isolate Pack Cache** — Isolate resource packs per-account to prevent tracking
- **Block Local URLs** — Block resource pack redirects to local/private addresses
- **Bypass Server Pack Requirement** — Let the user toggle required server resource pack(s) like client packs
- **Strip Mod Shader Overrides** — Strip server resource pack shader overrides targeting non-whitelisted mods
- **Key Resolution Protection** — Protect against key resolution mod detection in any server packet
- **Meteor Fix** — Disable Meteor Client's flawed key resolution protection
- **Mod Whitelist** — Automatically or manually exempt mods from protection
- **Chat Signing Control** — Configure chat message signing behavior
- **Account Manager** — Switch between Minecraft accounts using session tokens
- **Telemetry Blocking** — Disable data collection sent to Mojang

If you're interested in servers or plugins that are using tracking related exploits then look in the [Hall of Shame](https://github.com/NikOverflow/ExploitPreventer/blob/master/HALL_OF_SHAME.md).

## Requirements

- **Minecraft** 1.20 – 26.2
- **Fabric Loader** 0.16.0+ (0.18.5+ for MC 26.1.x)
- **Fabric API** (matching your Minecraft version)

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version
2. Download the latest [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version
3. Download the matching `nofingerprint-[minecraft_version]+v[version].jar` from official NoFingerprint releases
4. Place both mods in your `.minecraft/mods` folder
5. Launch Minecraft

If you previously used OpSec, the first launch copies `config/opsec.json` and `config/opsec-accounts.json` to `config/nofingerprint.json` and `config/nofingerprint-accounts.json` when the new files do not already exist. After that, NoFingerprint only reads and writes its own filenames.

## Configurations

The settings menu is accessible via the `NoFingerprint` button in the multiplayer server selection menu header or via [Mod Menu](https://modrinth.com/mod/modmenu).

If settings are changed while connected to a server it is recommended to reconnect to the server to ensure changes are applied.

#### Protection Tab

| Setting | Description |
|---------|-------------|
| **Spoof as vanilla** | Enable/disable [Spoof as Vanilla](#spoof-as-vanilla) |
| **Isolate Pack Cache** | Enable/disable [cache isolation](#isolate-pack-cache) |
| **Block Local Pack URLs** | Enable/disable [local URL blocking](#block-local-urls) |
| **Bypass Server Pack Requirement** | Configure [server pack bypass](#bypass-server-pack-requirement) behavior: • **MANUAL** (default): Default vanilla behavior on push. You can still toggle any server pack. • **ASK**: Server resource pack not applied but with consent screen to ask if the pack(s) should be applied • **ALWAYS ON**: Server resource pack not applied by default. You can still toggle any server pack |
| **Strip Mod Shader Overrides** | Enable/disable [shader override stripping](#strip-mod-shader-overrides) |
| **Clear Cache** | Delete all cached server resource packs |
| **Key Resolution Spoofing** | Enable/disable [key resolution protection](#key-resolution-protection) |
| **Fake Default Keybinds** | Return default vanilla keybind values instead of actual bindings |
| **Meteor Fix** | Disable Meteor Client's broken key resolution protection (only shown when Meteor is installed) |
| **Signing Mode** | Configure [chat signing](#chat-signing-control) behavior: • **OFF**: Strip signatures (maximum privacy) • **ON**: Default Minecraft behavior • **AUTO**: Only sign when required (recommended) |
| **Disable Telemetry** | Enable/disable [telemetry blocking](#telemetry-blocking) |

#### Whitelist Tab

| Setting | Description |
|---------|-------------|
| **Whitelist Mode** | Select whitelist behavior: • **BLOCK ALL**: All mod content blocked • **AUTO**: Mods with network channels are automatically whitelisted (default) • **CUSTOM**: Manually select which mods to whitelist |
| **Installed Mods** | Toggle individual mods ON/OFF to exempt them from protection (CUSTOM mode only) |

#### Miscellaneous Tab

| Setting | Description |
|---------|-------------|
| **Show Alerts** | Display chat messages when tracking is detected |
| **Show Toasts** | Display popup notifications for important events |
| **Log Detections** | Log all detection events to game log for transparency |
| **Debug Alerts** | Show alerts for all probed keys, even unchanged ones |
| **Debug Command** | Enable the `/nofingerprint` debug command. Off by default. |

#### Accounts Tab

| Setting | Description |
|---------|-------------|
| **Saved Accounts** | List of added accounts with login/logout and remove buttons |
| **Refresh All** | Revalidate all account tokens (invalid tokens marked red) |
| **Add Session Token** | Add a new account using a session (access) token |
| **Import** | Import accounts from a JSON file |
| **Export** | Export accounts to a JSON file |

### Debug Commands

The `/nofingerprint` command is **off by default** (enable it in Misc → Debug Command). When enabled, use `/nofingerprint` in-game to access debug information:

| Command | Description |
|---------|-------------|
| `/nofingerprint` | Show available commands |
| `/nofingerprint info` | Show overview of all tracked mods |
| `/nofingerprint info <mod>` | Show details for a specific mod (translation keys, keybinds, channels, known packs, shaders) |
| `/nofingerprint channels` | Show all tracked network channels with whitelist status |

### Understanding Alerts

- **Key Resolution Exploit Detected**: Server is probing your keys
- **Resource Pack Fingerprinting Detected**: Suspicious resource pack URL detected
- **Local URL Scan Detected**: Resource pack targeting your local/private address

## Feature Details

### Spoof as Vanilla

Servers can query your client brand to detect whether you're running a modded client. NoFingerprint provides vanilla spoofing by blocking all mod key resolutions, network channels, and known-pack identifiers (whilst keeping vanilla ones).

- **ON** - Appear as an unmodified Minecraft client
- **OFF** - Appear as a standard Fabric client (default)

Set to OFF by default to allow auto mod whitelisting (whitelist mods with network channels).

---

### Isolate Pack Cache

Based on [LiquidBounce](https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/java/net/ccbluex/liquidbounce/injection/mixins/minecraft/util/MixinDownloadQueue.java).

Server-required resource packs could be used to fingerprint a client instance across accounts.

https://alaggydev.github.io/posts/cytooxien/

Instead of storing all resource packs in a shared cache (`~/.minecraft/downloads/`), NoFingerprint creates separate cache directories for each account UUID.

---

### Block Local URLs

Derived from [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) by [NikOverFlow](https://github.com/NikOverflow)

Malicious servers can send resource pack URLs that redirect to your local network to probe for local devices and services.

https://alaggydev.github.io/posts/cytooxien/

NoFingerprint checks if a redirect or normal request targets a local address, then blocks the connection.

---

### Bypass Server Pack Requirement

Servers can push required resource packs the client is forced to apply. Declining them or toggling required server resource pack(s) is impossible on a vanilla client. Fake accepting them can be detectable via key resolution probing the client's resource pack key response.

Minecraft still accepts and downloads these packs as normal but NoFingerprint lets you toggle the pack textures at the client level. The language file of the server resource pack is preserved because servers can probe translation keys (e.g. via `{"translate": "some.pack.key"}`) to detect whether the pack is actually applied, and a vanilla client with the pack loaded would resolve those keys to the pack-defined value.

With NoFingerprint installed, server resource pack(s) appear as a normal user-toggleable entry in the resource pack menu so you can flip between stripped and fully-loaded.

**Modes:**
- **MANUAL** (default): Required packs apply fully like vanilla on push. Optional packs follow vanilla toggle semantics. The user can still unequip any server pack from the pack menu to strip it while keeping lang loaded.
- **ASK**: Required packs are stripped on push and a consent overlay prompts `[Continue]` / `[Load Pack For Real]`.
- **ALWAYS ON**: All server packs are stripped on push. No overlay. You can still toggle them back.

---

### Strip Mod Shader Overrides

Some mods (e.g. [Meteor Client](https://github.com/MeteorDevelopment/meteor-client)) render their GUI with their own shaders loaded through Minecraft's resource manager. A forced server resource pack can override the mod's own files to ship shaders under that mod to either blank the mod's GUI, crash the client with malformed shaders, or GPU DoS, which also fingerprints that the mod is installed.

NoFingerprint strips shader overrides under `assets/<mod>/shaders/` from server packs for any installed mod that isn't whitelisted, so the resource manager falls back to the mod's own bundled shaders. The rest of the pack still loads, so this keeps working even when a server forces the pack to make [Bypass Server Pack Requirement](#bypass-server-pack-requirement) unusable.

Vanilla (`minecraft`) shaders are never touched, and whitelisting a mod lets the server's shader override through.

---

### Key Resolution Protection

Servers can send translatable text containing keys like `key.attack` or `key.hide_icons` in any server packet to probe which keys you have bound or mod UI elements your client can resolve. This can reveal the client's installed mods.

https://wurst.wiki/sign_translation_vulnerability

NoFingerprint tracks when translation keys are being resolved during server packet processing and blocks Minecraft from resolving them based on your selected brand mode:

#### Spoof as Vanilla Behavior

- **ON**: Blocks all mod keys, returns default keybind values for vanilla keys
- **OFF**: Allows Fabric API and whitelisted mod keys, blocks everything else

When **Fake Default Keybinds** is disabled, vanilla keybinds resolve to their actual values.

---

### Meteor Fix

Legacy Meteor Client has a built-in key protection implementation which can lead to guaranteed detection with key resolution probing.

The server can use a specially crafted translation key probe with a fallback value. Instead of returning the fallback value a vanilla client would use, Meteor echoes the raw key.

NoFingerprint's fix is to blacklist Meteor's `AbstractSignEditScreenMixin` so NoFingerprint's protection can take over, including correct fallback handling.

---

### ExploitPreventer Compatibility

For users that prefer [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer)'s core protection implementation but still need NoFingerprint's additional features, both can be installed alongside each other. Overlapping features are automatically disabled to let EP handle them. Note that you would lose NoFingerprint features such as channel spoofing. The following NoFingerprint features are deferred to EP:

- Brand spoofing
- Channel spoofing
- Known-pack filtering
- Isolate pack cache
- Block local URLs
- Key resolution protection
- Mod whitelist

These settings are grayed out in the config screen but your saved preferences are preserved. If you remove EP later, they restore automatically.

Features that don't overlap remain fully functional: alerts, chat signing, account manager, telemetry blocking, [Strip Mod Shader Overrides](#strip-mod-shader-overrides), and [Meteor Fix](#meteor-fix).

---

### Channel Spoofing

Servers can query your registered network channels to detect which mods you have installed.

NoFingerprint can conditionally block mod channels that are registered with the server to prevent detection.
This is enabled by default; its behavior is controlled by the mod whitelist.

---

### Known-Pack Filtering

Servers can probe mod-injected pack identifiers that certain mods expose. NoFingerprint intercepts the outgoing `ServerboundSelectKnownPacks` response and strips entries belonging to non-whitelisted mods. Real vanilla and auto-whitelisted packs still pass through.

#### Spoof as Vanilla Behavior

- **ON**: Strips all mod-injected packs.
- **OFF**: Keeps packs for whitelisted mods, strips the rest.

> [!NOTE]
> Only active on clients where Fabric's known-packs hook is present (MC 1.21.11+ with modern fabric-api).

---

### Mod Whitelist

Some mods require server communication to function properly (e.g., VoiceChat, Xaero's Minimap quick travel). The whitelist allows you to exempt specific mods from channel spoofing, key resolution protection, known-pack filtering, and shader override stripping.

**Modes:**
- **OFF**: All mod content is blocked
- **AUTO** (default): Mods that register network channels are automatically whitelisted as they are the most likely to have server-side functionalities
- **CUSTOM**: Manually select which mods to whitelist from the installed mod list

When the whitelist is active (AUTO or CUSTOM), [Spoof as Vanilla](#spoof-as-vanilla) will be disabled as exposing Fabric mods would need the client brand to match accordingly.

> [!NOTE]
> CUSTOM mode lists every installed mod so any mod can be whitelisted; AUTO mode only shows mods that register network channels.

---

### Chat Signing Control

Based on [No Chat Reports](https://modrinth.com/mod/no-chat-reports).

Cryptographic signatures are attached to chat messages by default. Removing them makes it harder to associate chat messages with your Minecraft client and Microsoft account.

**Modes:**
- **OFF**: Strip all chat signatures, but prevents you from chatting in servers that enforce secure chat.
- **Auto**: Only sign messages when the server enforces secure chat.
- **ON**: Default Minecraft behavior, signs every message.

---

### Account Manager

Based on [Meteor Client](https://github.com/MeteorDevelopment/meteor-client).

Add Minecraft accounts with session tokens and switch between them without restarting the game.

- **Session Token Login** - Add accounts using access tokens
- **Refresh Token** - Fetch new session tokens for expired accounts
- **Offline Account** - Add username-only accounts without authentication
- **Account Switching** - Click an account to login, click again to logout to original account
- **Token Validation** - Refresh to check if tokens are still valid (expired tokens marked red)
- **Import/Export** - Backup and restore accounts via JSON files

> [!NOTE]
> Session tokens expire after some time. Use the Refresh button to check validity. Account files store access and refresh tokens in plaintext JSON.

---

### Telemetry Blocking

From [No Chat Reports](https://modrinth.com/mod/no-chat-reports).

Minecraft collects and sends telemetry data to Mojang, including:
- Game events and player actions
- Performance metrics
- Client configuration
- Usage statistics

NoFingerprint blocks telemetry sending to Mojang when telemetry blocking is enabled. Does not affect gameplay.

## Building from Source

### Prerequisites

- **Java 17** (1.20.1 – 1.20.4), **Java 21** (1.20.6 – 1.21.11), **Java 25** (26.1+)
- **Gradle** (included via wrapper)

### Building the Minecraft Mod

1. **Clone the repository**
   ```bash
   git clone https://github.com/dimaniojk/NoFingerprint.git
   cd NoFingerprint
   ```

2. **Build all versions**
   ```bash
   # Windows
   .\gradlew.bat build

   # Linux/Mac
   ./gradlew build
   ```

3. **Build a specific version**
   ```bash
   ./gradlew :1.20.1:build
   ./gradlew :1.20.2:build
   ./gradlew :1.20.4:build
   ./gradlew :1.20.6:build
   ./gradlew :1.21.1:build
   ./gradlew :1.21.4:build
   ./gradlew :1.21.6:build
   ./gradlew :1.21.9:build
   ./gradlew :1.21.11:build
   ./gradlew :26.1:build
   ./gradlew :26.2:build
   ```

Output JARs are located in `versions/<minecraft_version>/build/libs/`:

| Build Version | Supports |
|---------------|----------|
| 1.20.1 | 1.20 – 1.20.1 |
| 1.20.2 | 1.20.2 |
| 1.20.4 | 1.20.3 – 1.20.4 |
| 1.20.6 | 1.20.5 – 1.20.6 |
| 1.21.1 | 1.21 – 1.21.1 |
| 1.21.4 | 1.21.2 – 1.21.5 |
| 1.21.6 | 1.21.6 – 1.21.8 |
| 1.21.9 | 1.21.9 – 1.21.10 |
| 1.21.11 | 1.21.11 |
| 26.1 | 26.1 – 26.1.2 |
| 26.2 | 26.2 |

Example artifact name: `nofingerprint-1.21.11+v1.1.7.1-nofingerprint.1.jar`

## Upstream / Attribution

NoFingerprint is based on OpSec by aurickk.

Original project:
https://github.com/aurickk/OpSec

NoFingerprint continues development independently from the archived upstream project. The original author does not endorse this fork.

See `NOTICE` and `LICENSE` for copyright and GPL-3.0 terms.

## References

- [ExploitPreventer](https://github.com/NikOverflow/ExploitPreventer) - Local URL blocking and server key resolution protection anti-measures
- [LiquidBounce](https://github.com/CCBlueX/LiquidBounce/blob/nextgen/src/main/java/net/ccbluex/liquidbounce/injection/mixins/minecraft/util/MixinDownloadQueue.java) - Cached server resource pack isolation
- [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) - Session token sign in
- [No Chat Reports](https://modrinth.com/mod/no-chat-reports) - Chat signing control and telemetry blocking
- [No Prying Eyes](https://github.com/Daxanius/NoPryingEyes?tab=readme-ov-file) - Secure chat enforcement detection
- [MixinSquared](https://github.com/Bawnorton/MixinSquared) - Mixin cancellation for Meteor Fix
- [Stonecutter](https://stonecutter.kikugie.dev/) - Multi-version build system
- [Fabric API](https://github.com/FabricMC/fabric-api) - Fabric translation and keybind keys

## Disclaimer

NoFingerprint is a privacy tool designed to protect players from unwanted client fingerprinting and tracking. It is not intended or encouraged for use in bypassing server rules, evading bans, or gaining unfair advantages. Users are responsible for complying with the rules and terms of service of any server they connect to.
