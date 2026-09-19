# NoFingerprint 1.1.7.1-nofingerprint.2-beta.1

Public **beta**. Not a stable/production-ready release.

## What is NoFingerprint?

NoFingerprint is a client-side Fabric privacy mod that reduces unnecessary client fingerprinting and information disclosure to Minecraft servers.

It is an independently maintained GPL-3.0 fork of [OpSec](https://github.com/aurickk/OpSec) by aurickk. It is not guaranteed to be undetectable, it is not an anti-cheat bypass, and it is not a guarantee of anonymity.

## Highlights

- Fork identity: `nofingerprint`, separate configs, GitHub project URLs
- ExploitPreventer can be installed together without disabling every NoFingerprint feature
- Explicit private/local CIDR checks, including CGNAT and IPv6 unique-local addresses
- Local resource-pack URL blocking on Minecraft 1.20.1 (implemented; not live-verified in this beta)
- HTTP 308 redirect hops are classified, not ignored
- Original geometric Mod Menu icon (replaces the upstream OP mark)

## Supported versions

This beta ships JARs for:

- Minecraft **1.20 – 1.20.1** (Java 17)
- Minecraft **1.21.11** (Java 21)
- Minecraft **26.2** (Java 25)

Other Stonecutter targets can be built from source. **Build verified** is not the same as **runtime verified**. Most protections have not been confirmed on a live Minecraft session. Minecraft 26.2 has no protocol-library probe coverage yet.

## Important limitations

- Spoof as Vanilla is **off** by default; Fabric identity may remain visible
- AUTO channel whitelist can reveal networking mods
- DNS rebinding is only partially mitigated
- Live probe coverage is incomplete
- Account Manager stores tokens **unencrypted** on disk

## Security notice

Treat `config/nofingerprint-accounts.json` as a secret. Anyone who can read it can use saved session tokens.

Download only from official NoFingerprint GitHub releases once they exist. Verify SHA-256 against `release/SHA256SUMS.txt`.

Details: [SECURITY_AUDIT.md](SECURITY_AUDIT.md).

## Upstream attribution

Based on OpSec by aurickk: https://github.com/aurickk/OpSec

See `NOTICE` and `LICENSE`.

## Downloads

Attach the JARs listed in `release/SHA256SUMS.txt` to the GitHub release. Do not use placeholder or third-party URLs.
