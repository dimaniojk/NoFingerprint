'use strict'

const mc = require('minecraft-protocol')
const minecraftData = require('minecraft-data')
const { Report } = require('./report')
const { createPackHttp } = require('./httpPack')
const {
  decodeBrand,
  decodeRegister,
  protocolSupportsKnownPacks,
  sendJoin,
  sendPosition,
  sendResourcePack,
  sendKnownPacks,
  sendCustomPayload,
  sendOpenSign
} = require('./packets')
const { evaluateSession } = require('./evaluate')

const OFFERED_PACKS = [
  { namespace: 'minecraft', id: 'core', version: '1.21.11' },
  { namespace: 'fabric', id: 'resources', version: '1.0.0' },
  { namespace: 'modmenu', id: 'modmenu', version: '1.0.0' },
  { namespace: 'nofingerprint_test', id: 'fake', version: '1.0.0' }
]

function expectedFor (configuration) {
  switch (configuration) {
    case 'baseline':
      return {
        brand: 'fabric',
        channelsNote: 'Fabric register list may include fabric-api channels',
        inboundStayConnected: false,
        knownPacks: 'may-include-mods',
        blockLocalPack: false,
        expectLocalGet: true,
        translation: 'mod-like keys may resolve',
        chat: 'vanilla signing if keys exist'
      }
    case 'strict':
      return {
        brand: 'vanilla',
        channelsNote: 'no mod channels; no nofingerprint channel',
        inboundStayConnected: true,
        knownPacks: 'minecraft-only',
        blockLocalPack: true,
        expectLocalGet: false,
        translation: 'fake mod keys must not resolve to a unique value',
        chat: 'SIGN default unless user changed it'
      }
    case 'ep':
      return {
        brand: 'vanilla-or-fabric-per-nf-toggle',
        channelsNote: 'NF brand/channels must still work with EP loaded',
        inboundStayConnected: true,
        knownPacks: 'minecraft-only-if-spoof',
        blockLocalPack: true,
        expectLocalGet: false,
        translation: 'EP owns translation wrap',
        chat: 'NF chat signing still independent'
      }
    case 'self-test':
      return {
        brand: '*',
        channelsNote: 'harness bot is not Fabric',
        inboundStayConnected: false,
        knownPacks: 'any',
        blockLocalPack: false,
        expectLocalGet: false,
        translation: 'n/a',
        chat: 'n/a',
        allowDisconnect: true
      }
    case 'default':
    default:
      return {
        brand: 'fabric',
        channelsNote: 'default spoofAsVanilla=false: Fabric identity may remain visible; AUTO whitelist may leak mod channels',
        inboundStayConnected: true,
        knownPacks: 'may-include-mods',
        blockLocalPack: true,
        expectLocalGet: false,
        translation: 'fake mod keys must not resolve',
        chat: 'SIGN default'
      }
  }
}

function attachSession (client, ctx) {
  const t0 = ctx.report.t0
  const session = {
    username: client.username,
    uuid: client.uuid,
    protocolVersion: client.protocolVersion,
    version: client.version,
    brand: null,
    brandChannel: null,
    brandT: null,
    channels: [],
    customPayloads: [],
    knownPacks: [],
    knownPacksReceived: false,
    packStatus: [],
    chatSession: [],
    chatMessages: [],
    signUpdates: [],
    inboundOutcome: { disconnected: false, reason: null },
    lifecycle: [],
    sawPlay: false,
    sawPackStatus: false,
    endReason: null,
    cacheNote: null
  }

  const mark = (name, extra) => {
    session.lifecycle.push({ t_ms: Date.now() - t0, name, ...extra })
    ctx.report.event(name, { username: client.username, ...extra })
  }

  mark('tcp_or_client_object', { remote: client.socket && client.socket.remoteAddress })

  client.on('state', (newState, oldState) => {
    mark('state', { from: oldState, to: newState })
    if (newState === 'play') session.sawPlay = true
    if (newState === 'configuration' && protocolSupportsKnownPacks(ctx.version)) {
      const sent = sendKnownPacks(client, OFFERED_PACKS, ctx.mcData)
      mark('known_packs_offered', { ok: sent.ok, name: sent.name, errors: sent.errors })
    }
  })

  client.on('packet', (data, meta, buffer) => {
    const rec = {
      t_ms: Date.now() - t0,
      state: meta.state,
      name: meta.name,
      direction: 'c2s',
      size: buffer ? buffer.length : 0
    }
    session.lifecycle.push({ ...rec, kind: 'packet' })

    if (meta.name === 'custom_payload' || meta.name === 'plugin_message') {
      const channel = data.channel || data.name
      const payload = data.data || data.payload
      const length = payload && payload.length != null ? payload.length : 0
      session.customPayloads.push({
        channel,
        direction: 'c2s',
        length,
        state: meta.state,
        t_ms: rec.t_ms
      })
      if (channel === 'minecraft:brand' || channel === 'MC|Brand') {
        session.brand = decodeBrand(payload) || (typeof payload === 'string' ? payload : session.brand)
        session.brandChannel = channel
        session.brandT = rec.t_ms
      }
      if (channel === 'minecraft:register' || channel === 'REGISTER') {
        const list = decodeRegister(payload)
        for (const c of list) {
          if (!session.channels.includes(c)) session.channels.push(c)
        }
      }
    }

    if (meta.name === 'brand' && data && (data.brand || data.name)) {
      session.brand = data.brand || data.name
      session.brandChannel = 'brand'
      session.brandT = rec.t_ms
    }

    if (meta.name === 'select_known_packs' || meta.name === 'known_packs') {
      session.knownPacksReceived = true
      session.knownPacks = data.knownPacks || data.packs || data.array || data
    }

    if (String(meta.name).includes('resource_pack')) {
      session.sawPackStatus = true
      session.packStatus.push({ t_ms: rec.t_ms, name: meta.name, data })
    }

    if (meta.name === 'chat_session_update' || meta.name === 'player_session') {
      session.chatSession.push({ t_ms: rec.t_ms, name: meta.name, keys: Object.keys(data || {}) })
    }

    if (meta.name === 'chat_message' || meta.name === 'chat' || meta.name === 'chat_command') {
      session.chatMessages.push({
        t_ms: rec.t_ms,
        name: meta.name,
        message: data.message || data.command,
        hasSignature: !!(data.signature || data.messageSignature)
      })
    }

    if (meta.name === 'update_sign' || meta.name === 'sign_update') {
      session.signUpdates.push({ t_ms: rec.t_ms, name: meta.name, data })
    }
  })

  client.on('end', (reason) => {
    session.endReason = reason
    if (!session.inboundOutcome.disconnected && ctx.inboundSent) {
      session.inboundOutcome.disconnected = true
      session.inboundOutcome.reason = reason
    }
    ctx.report.disconnects.push({ username: client.username, reason, t_ms: Date.now() - t0 })
    mark('end', { reason })
  })

  client.on('error', (err) => {
    mark('error', { message: err.message })
  })

  return session
}

async function sleep (ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function runPlayProbes (client, session, ctx) {
  session.sawPlay = true
  ctx.report.event('play_probes_start', { username: client.username })

  sendJoin(client, ctx.server, ctx.mcData)
  sendPosition(client, ctx.mcData)

  await sleep(400)

  const inboundChannels = [
    { channel: 'nofingerprint_probe:unknown', data: Buffer.from([0x00]) },
    { channel: 'fabric:registry/sync', data: Buffer.alloc(0) },
    { channel: 'fabric:s2c/not_a_real_packet', data: Buffer.from('x') },
    { channel: 'voicechat:secret', data: Buffer.from([0x01, 0x02]) }
  ]
  ctx.inboundSent = true
  const inboundSent = []
  for (const p of inboundChannels) {
    const r = sendCustomPayload(client, p.channel, p.data, ctx.mcData, 'play')
    inboundSent.push({ channel: p.channel, ok: r.ok, name: r.name, errors: r.errors })
  }
  session.inboundOutcome.sent = inboundSent
  await sleep(800)

  const packUrl = `${ctx.http.snapshot().baseUrl}/test.zip`
  const pack = sendResourcePack(client, packUrl, null, ctx.mcData)
  ctx.report.event('resource_pack_sent', { url: packUrl, ok: pack.ok, packet: pack.name, errors: pack.errors })

  await sleep(600)
  const redirectUrl = `${ctx.http.snapshot().baseUrl}/to-loopback-308`
  sendResourcePack(client, redirectUrl, null, ctx.mcData)
  ctx.report.event('resource_pack_308_sent', { url: redirectUrl })

  await sleep(400)
  sendOpenSign(client, ctx.mcData)
  try {
    client.write('system_chat', {
      content: JSON.stringify({
        translate: 'nofingerprint_test.fake_mod_key',
        fallback: 'UNRESOLVED_FALLBACK'
      }),
      overlay: false,
      isActionBar: false
    })
  } catch {
    /* optional */
  }

  await sleep(ctx.settleMs)
}

function createProbeServer (opts) {
  const version = opts.version
  const supported = mc.supportedVersions || []
  if (version && !supported.includes(version) && version !== 'false') {
    const err = new Error(`minecraft-protocol ${require('minecraft-protocol/package.json').version} does not list ${version}. supported=${supported.join(', ')}`)
    err.code = 'NOT_SUPPORTED'
    throw err
  }

  const report = new Report({
    minecraftVersion: version,
    configuration: opts.configuration,
    host: opts.host,
    port: opts.port,
    jsonlPath: opts.jsonlPath,
    nofingerprintVersion: opts.nofingerprintVersion
  })

  const http = createPackHttp({ host: '127.0.0.1' })
  const mcData = minecraftData(version)
  const expected = expectedFor(opts.configuration)

  const server = mc.createServer({
    'online-mode': false,
    host: opts.host,
    port: opts.port,
    version,
    motd: `NF probe ${version} config=${opts.configuration}`,
    maxPlayers: 8,
    keepAlive: true,
    hideErrors: false,
    enforceSecureProfile: false,
    validateChannelProtocol: false,
    errorHandler: (client, error) => {
      report.event('client_error', { username: client.username, message: error.message })
      console.error('[server] client error', client.username, error.message)
    }
  })

  const ctx = { report, http, version, configuration: opts.configuration, expected, server, mcData, inboundSent: false, settleMs: opts.settleMs || 4000 }
  const sessions = []

  server.on('connection', (client) => {
    report.event('connection', { remote: client.socket && client.socket.remoteAddress })
  })

  server.on('login', (client) => {
    const session = attachSession(client, ctx)
    sessions.push(session)
    report.event('login', { username: client.username, uuid: client.uuid, protocol: client.protocolVersion })
    if (protocolSupportsKnownPacks(version) && client.state === 'configuration') {
      sendKnownPacks(client, OFFERED_PACKS, ctx.mcData)
    }
  })

  server.on('playerJoin', (client) => {
    const session = sessions.find((s) => s.username === client.username) || attachSession(client, ctx)
    report.event('playerJoin', { username: client.username })
    runPlayProbes(client, session, ctx).then(() => {
      evaluateSession(session, ctx)
      if (opts.once) {
        try { client.end('nf-probe-complete') } catch { /* ignore */ }
        setTimeout(() => server.close(), 500)
      }
    }).catch((err) => {
      console.error('[server] probe sequence failed', err)
      report.add({
        probe: 'probe_sequence',
        observed: err.message,
        expected: 'complete',
        result: 'FAIL',
        notes: err.stack
      })
    })
  })

  server.on('error', (err) => {
    console.error('[server] error', err)
  })

  return { server, report, http, ctx, sessions, supported }
}

module.exports = { createProbeServer, expectedFor, OFFERED_PACKS }
