# Changelog

## 1.1.7.1-nofingerprint.2-beta.1

First public **beta** of the independently maintained NoFingerprint fork. Not a stable release.

### Fork / branding

- Rebrand from [OpSec](https://github.com/aurickk/OpSec) by aurickk: mod id `nofingerprint`, package `io.github.dimaniojk.nofingerprint`, config filenames, GitHub URLs.
- GPL-3.0-only license retained; `NOTICE` records upstream attribution.
- Upstream OpSec “OP” icon replaced with an original geometric mark (not a finished brand logo).
- Account Manager UI warns that tokens are stored unencrypted.

### Privacy hardening

- ExploitPreventer coexistence is capability-based: only overlapping HTTP and translation hooks stand down. Brand, channels, known packs, cache isolation, and other NF-only features stay available.
- Local/private resource-pack URL classification uses explicit CIDR rules, including CGNAT (`100.64.0.0/10`) and IPv6 unique-local (`fc00::/7`).
- Minecraft 1.20.1 local pack URL blocking is implemented in code (not live-tested against a real client in this release).
- HTTP 308 (and other 3xx hops in the implemented set) are validated per hop.
- DNS TOCTOU / rebinding remains **PARTIALLY MITIGATED** and is not claimed fixed.

### Compatibility

- `fabric.mod.json` no longer `conflicts` with ExploitPreventer.
- No Chat Reports / No Prying Eyes still own chat-signing UI when loaded.

### Testing

- Unit/regression tests for address classification, redirects, and compatibility policy.
- Local protocol probe harness added at `tools/probe-server/` (developer tooling, not shipped in the JAR).
- Representative **builds** for Minecraft 1.20.1, 1.21.11, and 26.2.
- Live Fabric play-state probing is **incomplete**. Protocol-library self-tests are not a Fabric client. Minecraft 26.2 has no probe-library support.

### Known limitations

- `spoofAsVanilla` remains **off** by default.
- AUTO channel whitelist can reveal networking mods.
- Account tokens remain plaintext (no encryption in this release).
- Runtime verification of brand, channels, known packs, translation echo, local URL GET suppression, 308 follow, cache isolation, and EP dual-install is still **CODE VERIFIED ONLY** unless tagged otherwise in `RUNTIME_PROBE_RESULTS.md`.
