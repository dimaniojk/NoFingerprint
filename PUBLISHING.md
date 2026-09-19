# Publishing

NoFingerprint **1.1.7.1-nofingerprint.2-beta.1** is a **beta**, not a stable release.

Do not upload to CurseForge or Modrinth until:

1. The GitHub repository `dimaniojk/NoFingerprint` exists and this tree is pushed.
2. Release JARs and `release/SHA256SUMS.txt` match the files you attach.
3. You have read the derivative-work and (if applicable) AI-assisted-development disclosures below.

This document does not authorize a push. Run remote git commands only when you intend to publish.

## GitHub

Required:

- GPL-3.0 `LICENSE`
- Full source
- `NOTICE` / upstream attribution
- Release JARs (one per shipped Minecraft line)
- `CHANGELOG.md` and `RELEASE_NOTES_1.1.7.1-nofingerprint.2-beta.1.md`
- `release/SHA256SUMS.txt`

Recommended tag (must match `JarIntegrityChecker`, prefix `v`):

```
v1.1.7.1-nofingerprint.2-beta.1
```

Suggested local checks (do not skip hooks):

```bash
git status
git diff
git log --oneline --decorate -n 15
```

This clone currently has `upstream` = `https://github.com/aurickk/OpSec.git` and **no** `origin` for `dimaniojk/NoFingerprint`. After creating the GitHub repo:

```bash
git remote add origin https://github.com/dimaniojk/NoFingerprint.git
git push -u origin HEAD
git tag -a v1.1.7.1-nofingerprint.2-beta.1 -m "NoFingerprint 1.1.7.1-nofingerprint.2-beta.1"
git push origin v1.1.7.1-nofingerprint.2-beta.1
```

Create the GitHub Release with the three JARs and SHA256SUMS. Do not force-push `main`.

## CurseForge

Upload the same JARs as GitHub. Mark the file as **Beta**.

Suggested description (plain text / markdown):

```
NoFingerprint is a client-side Fabric privacy mod that reduces unnecessary
client fingerprinting and information disclosure to Minecraft servers.

This is an independently maintained fork of OpSec by aurickk.
Original project: https://github.com/aurickk/OpSec
License: GPL-3.0-only

NoFingerprint is not OpSec. Significant changes include a separate project
identity, capability-based ExploitPreventer coexistence, explicit CIDR-based
private/local URL classification (including CGNAT and IPv6 ULA), 1.20.1 local
pack URL blocking, HTTP 308 hop validation, and additional tests. Live runtime
verification of many protections is still incomplete; this upload is a beta.

This is not an anti-cheat bypass and is not guaranteed to be undetectable.
```

Do not claim original authorship of the OpSec-derived codebase. Credit aurickk.

## Modrinth

Mark the version as **beta**. Link the source repository. License: GPL-3.0.

### Derivative content

This project is a **derivative** of OpSec (GPL-3.0) by aurickk:
https://github.com/aurickk/OpSec

Disclose that on the project page. Do not present NoFingerprint as an original work from scratch.

### AI-assisted development

Parts of this fork (rebrand, hardening, probe harness, documentation, and the current geometric icon) were produced with AI coding assistance. If Modrinth’s current rules require disclosing AI-generated or AI-assisted content, disclose it on the project and on this version. Do not omit that disclosure to pass moderation.

Suggested project blurb:

```
Client-side Fabric privacy mod. Independently maintained GPL-3.0 fork of
OpSec by aurickk. Beta: not undetectable, not an anti-cheat bypass.
Source: https://github.com/dimaniojk/NoFingerprint
```

## Probe harness

`tools/probe-server/` is developer tooling. Do not attach it as a “mod file” on CurseForge or Modrinth.
