'use strict'

const { decodeBrand, decodeRegister, protocolSupportsKnownPacks, protocolHasTypedPayloads } = require('./packets')

function containsNofingerprintChannel (channels) {
  return channels.some((c) => String(c).toLowerCase().includes('nofingerprint'))
}

function evaluateSession (session, ctx) {
  const { report, http, version, configuration, expected } = ctx
  const channels = session.channels.slice()
  const brand = session.brand
  const knownPacks = session.knownPacks
  const packStatus = session.packStatus
  const chatSession = session.chatSession
  const customPayloads = session.customPayloads
  const inboundOutcome = session.inboundOutcome
  const signUpdates = session.signUpdates
  const endReason = session.endReason
  const httpSnap = http.snapshot()

  const brandExpected = expected.brand
  report.add({
    probe: 'client_brand',
    observed: brand,
    expected: brandExpected,
    result: brand == null
      ? (brandExpected === '*' ? 'INCONCLUSIVE' : (session.sawPlay ? 'FAIL' : 'INCONCLUSIVE'))
      : (brandExpected == null || brandExpected === '*'
        ? 'PASS'
        : (String(brand).toLowerCase() === String(brandExpected).toLowerCase() ? 'PASS' : 'FAIL')),
    notes: brand == null
      ? 'No minecraft:brand payload was received. Silence is not treated as PASS.'
      : `channel=${session.brandChannel || 'minecraft:brand'} t_ms=${session.brandT}`
  })

  report.add({
    probe: 'custom_payload_channels',
    observed: {
      channels,
      payloads: customPayloads.map((p) => ({
        channel: p.channel,
        direction: p.direction,
        length: p.length,
        state: p.state,
        t_ms: p.t_ms
      }))
    },
    expected: expected.channelsNote,
    result: containsNofingerprintChannel(channels) ? 'FAIL' : (channels.length || brand ? 'PASS' : 'INCONCLUSIVE'),
    notes: containsNofingerprintChannel(channels)
      ? 'Client advertised a nofingerprint channel.'
      : (channels.length === 0 ? 'No minecraft:register list observed (vanilla clients often send none).' : `${channels.length} registered channel(s).`)
  })

  if (!protocolHasTypedPayloads(version)) {
    report.add({
      probe: 'inbound_unknown_payload',
      observed: inboundOutcome,
      expected: 'NOT_SUPPORTED on <1.20.5 (no typed Fabric codec)',
      result: 'NOT_SUPPORTED',
      notes: inboundOutcome && inboundOutcome.disconnected
        ? `Client disconnected: ${inboundOutcome.reason || endReason || ''}`
        : 'Typed payload codecs do not exist on this protocol.'
    })
  } else {
    const disc = !!(inboundOutcome && inboundOutcome.disconnected)
    report.add({
      probe: 'inbound_unknown_payload',
      observed: inboundOutcome,
      expected: expected.inboundStayConnected ? 'client stays connected (vanilla-like ignore)' : 'document disconnect vs ignore',
      result: expected.inboundStayConnected
        ? (disc ? 'FAIL' : 'PASS')
        : 'INCONCLUSIVE',
      notes: disc
        ? `Disconnect after inbound probe: ${inboundOutcome.reason || endReason || 'unknown'}`
        : (expected.inboundStayConnected
          ? 'Client remained connected after unknown/malformed custom payloads.'
          : 'Stay-connected is recorded but not scored as NF codec protection unless the client is a real Fabric/NoFingerprint session.')
    })
  }

  if (!protocolSupportsKnownPacks(version)) {
    report.add({
      probe: 'known_packs',
      observed: knownPacks,
      expected: 'NOT_SUPPORTED',
      result: 'NOT_SUPPORTED',
      notes: 'select_known_packs does not exist before Minecraft 1.20.5.'
    })
  } else if (!session.knownPacksReceived) {
    report.add({
      probe: 'known_packs',
      observed: knownPacks,
      expected: expected.knownPacks,
      result: 'INCONCLUSIVE',
      notes: 'Client never sent select_known_packs. The library may have skipped the configuration offer, or the client ignored it.'
    })
  } else {
    const ns = (knownPacks || []).map((p) => p.namespace || p.knownPacksNamespace)
    const leakedMod = ns.some((n) => n && n !== 'minecraft')
    let result = 'INCONCLUSIVE'
    if (expected.knownPacks === 'minecraft-only') {
      result = leakedMod ? 'FAIL' : 'PASS'
    } else if (expected.knownPacks === 'may-include-mods') {
      result = 'PASS'
    }
    report.add({
      probe: 'known_packs',
      observed: knownPacks,
      expected: expected.knownPacks,
      result,
      notes: leakedMod ? 'Non-minecraft namespaces present.' : 'Only minecraft namespaces, or empty intersection.'
    })
  }

  const localGets = http.countsFor('/test.zip')
  const privateGets = http.countsFor('/private')
  let packResult = 'INCONCLUSIVE'
  if (expected.blockLocalPack) {
    if (localGets.httpCount > 0) packResult = 'FAIL'
    else if (session.sawPackStatus || session.sawPlay) packResult = 'PASS'
    else packResult = 'INCONCLUSIVE'
  } else if (expected.expectLocalGet) {
    packResult = localGets.httpCount > 0 ? 'PASS' : 'FAIL'
  }
  report.add({
    probe: 'resource_pack_local_url',
    observed: {
      tcpCount: httpSnap.tcpCount,
      httpCount: httpSnap.httpCount,
      testZipGets: localGets.httpCount,
      privateGets: privateGets.httpCount,
      packStatus,
      requests: httpSnap.requests.map((r) => ({ method: r.method, url: r.url, headers: r.headers }))
    },
    expected: expected.blockLocalPack
      ? 'no HTTP GET to the local pack listener'
      : 'GET /test.zip (baseline Fabric/vanilla download)',
    result: packResult,
    notes: expected.blockLocalPack
      ? (localGets.httpCount > 0
        ? 'Local HTTP service received a GET — block did not happen before the request.'
        : (httpSnap.tcpCount > 0
          ? 'TCP connect(s) recorded but no GET /test.zip. Treat as partial (handshake without request).'
          : 'No TCP connect and no HTTP GET to the harness listener.'))
      : `GET count=${localGets.httpCount}`
  })

  report.add({
    probe: 'resource_pack_status',
    observed: packStatus,
    expected: 'record only',
    result: packStatus.length ? 'PASS' : 'INCONCLUSIVE',
    notes: packStatus.length
      ? 'Status packets captured. Differences vs vanilla are fingerprint surfaces, not scored as FAIL.'
      : 'No resource-pack status packet from the client.'
  })

  report.add({
    probe: 'redirect_308',
    observed: {
      tcpCount: httpSnap.tcpCount,
      privateGets: privateGets.httpCount,
      chainGets: http.countsFor('/chain').httpCount,
      redirect308: http.countsFor('/to-loopback-308').httpCount
    },
    expected: 'private target /private receives no GET; 308 hop classified',
    result: privateGets.httpCount > 0
      ? 'FAIL'
      : (http.countsFor('/to-loopback-308').httpCount > 0 || http.countsFor('/chain').httpCount > 0
        ? 'PASS'
        : 'INCONCLUSIVE'),
    notes: privateGets.httpCount > 0
      ? 'Private endpoint received an HTTP request after redirect.'
      : 'Initial pack URL for 308 is loopback in this harness, so a successful NF block may skip the redirect hop entirely. Unit tests cover RedirectPolicy; live 308-from-public remains limited by bindable addresses.'
  })

  report.add({
    probe: 'translation_sign_echo',
    observed: signUpdates,
    expected: expected.translation,
    result: signUpdates.length ? 'INCONCLUSIVE' : 'INCONCLUSIVE',
    notes: signUpdates.length
      ? 'Client sent update_sign. Compare lines to keys vs resolved values (needs a player closing the sign).'
      : 'No update_sign. Automated clients never close the editor; this probe needs a human or is INCONCLUSIVE.'
  })

  report.add({
    probe: 'keybind_sign_echo',
    observed: signUpdates,
    expected: 'document resolved vs raw key.jump / key.fake_mod.open_gui',
    result: 'INCONCLUSIVE',
    notes: 'Same observation path as translation (sign editor). Not scored without an echoed line.'
  })

  report.add({
    probe: 'chat_signing',
    observed: {
      chatSession,
      chatMessages: session.chatMessages
    },
    expected: expected.chat,
    result: 'INCONCLUSIVE',
    notes: 'Offline-mode sessions usually have no Mojang profile key. Presence/absence of chat_session_update is recorded, not scored.'
  })

  report.add({
    probe: 'cache_isolation',
    observed: session.cacheNote || null,
    expected: 'Account B must not reuse Account A cache (second username → second GET)',
    result: 'INCONCLUSIVE',
    notes: 'Requires two sequential joins with different offline usernames against the same pack URL/hash. Use --cache-pair or connect twice.'
  })

  report.add({
    probe: 'dns_toctou',
    observed: null,
    expected: 'not claimed as fixed',
    result: 'INCONCLUSIVE',
    notes: 'HttpURLConnection may resolve twice. A safe deterministic rebind against the JVM was not constructed (would need a fake DNS for the Minecraft process). See SECURITY_AUDIT.md PARTIALLY MITIGATED.'
  })

  report.add({
    probe: 'connection_lifecycle',
    observed: session.lifecycle,
    expected: 'timestamps only',
    result: session.lifecycle.length ? 'PASS' : 'INCONCLUSIVE',
    notes: 'Timing is collected, not obfuscated.'
  })

  if (endReason) {
    report.add({
      probe: 'disconnect',
      observed: endReason,
      expected: expected.allowDisconnect ? 'any' : 'stay connected through probes',
      result: expected.allowDisconnect ? 'PASS' : 'INCONCLUSIVE',
      notes: String(endReason)
    })
  }
}

module.exports = { evaluateSession, containsNofingerprintChannel, decodeBrand, decodeRegister }
