'use strict'

const crypto = require('crypto')

function uuidv4 () {
  return crypto.randomUUID()
}

function decodeBrand (buf) {
  if (!buf || !Buffer.isBuffer(buf) || buf.length === 0) return null
  try {
    let i = 0
    let n = 0
    let shift = 0
    while (i < buf.length) {
      const b = buf[i++]
      n |= (b & 0x7f) << shift
      if ((b & 0x80) === 0) break
      shift += 7
    }
    return buf.slice(i, i + n).toString('utf8')
  } catch {
    return buf.toString('utf8').replace(/[^\x20-\x7e]/g, '')
  }
}

function decodeRegister (buf) {
  if (!buf || !Buffer.isBuffer(buf)) return []
  const body = buf[0] < 32 ? buf.slice(1) : buf
  return body.toString('utf8').split('\0').map((s) => s.trim()).filter(Boolean)
}

function chatComponent (mcData, text) {
  const nbt = require('prismarine-nbt')
  if (mcData.supportFeature && mcData.supportFeature('chatPacketsUseNbtComponents')) {
    return nbt.comp({ text: nbt.string(text) })
  }
  return JSON.stringify({ text })
}

function translatableComponent (key, fallback) {
  return JSON.stringify({
    translate: key,
    fallback: fallback || key
  })
}

function clientboundNames (mcData, state) {
  try {
    const t = mcData.protocol[state].toClient.types.packet
    return Object.values(t[1][0].type[1].mappings)
  } catch {
    return []
  }
}

function serverboundNames (mcData, state) {
  try {
    const t = mcData.protocol[state].toServer.types.packet
    return Object.values(t[1][0].type[1].mappings)
  } catch {
    return []
  }
}

function tryWrite (client, names, params, mcData, state) {
  const list = Array.isArray(names) ? names : [names]
  const allowed = mcData && state ? new Set(clientboundNames(mcData, state)) : null
  const errors = []
  for (const name of list) {
    if (allowed && !allowed.has(name)) {
      errors.push(`${name}: not in ${state} clientbound`)
      continue
    }
    try {
      client.write(name, params)
      return { ok: true, name }
    } catch (err) {
      errors.push(`${name}: ${err.message}`)
    }
  }
  return { ok: false, errors }
}

function protocolSupportsKnownPacks (version) {
  const parts = String(version).split('.').map((x) => parseInt(x, 10) || 0)
  if (parts[0] > 1) return true
  if (parts[0] === 1 && parts[1] > 20) return true
  if (parts[0] === 1 && parts[1] === 20 && parts[2] >= 5) return true
  return false
}

function protocolHasTypedPayloads (version) {
  return protocolSupportsKnownPacks(version)
}

function protocolHasConfigPhase (version) {
  const parts = String(version).split('.').map((x) => parseInt(x, 10) || 0)
  if (parts[0] > 1) return true
  if (parts[0] === 1 && parts[1] > 20) return true
  if (parts[0] === 1 && parts[1] === 20 && parts[2] >= 2) return true
  return false
}

function sha1Hex (s) {
  return crypto.createHash('sha1').update(s).digest('hex')
}

function sendJoin (client, server, mcData) {
  const loginPacket = mcData.loginPacket || {}
  return tryWrite(client, 'login', {
    ...loginPacket,
    entityId: client.id || 1,
    isHardcore: false,
    gameMode: 0,
    previousGameMode: 1,
    worldNames: loginPacket.worldNames || ['minecraft:overworld'],
    dimensionCodec: loginPacket.dimensionCodec,
    worldType: loginPacket.worldType || 'minecraft:overworld',
    worldName: 'minecraft:overworld',
    hashedSeed: [0, 0],
    maxPlayers: server.maxPlayers || 8,
    viewDistance: 8,
    simulationDistance: 8,
    reducedDebugInfo: false,
    enableRespawnScreen: true,
    isDebug: false,
    isFlat: true,
    portalCooldown: 0,
    doLimitedCrafting: false,
    enforcesSecureChat: false,
    enforceSecureChat: false,
    seaLevel: 63
  }, mcData, 'play')
}

function sendPosition (client, mcData) {
  return tryWrite(client, ['position', 'synchronize_player_position'], {
    x: 0,
    y: 80,
    z: 0,
    yaw: 0,
    pitch: 0,
    flags: 0,
    teleportId: 1,
    dismountVehicle: false
  }, mcData, 'play')
}

function sendResourcePack (client, url, hash, mcData) {
  const uuid = uuidv4()
  const hashed = hash || sha1Hex(url)
  const attempts = [
    {
      names: ['add_resource_pack', 'resource_pack_push'],
      params: {
        uuid,
        url,
        hash: hashed,
        forced: true
      }
    },
    {
      names: ['resource_pack_send'],
      params: {
        url,
        hash: hashed,
        forced: true
      }
    }
  ]
  for (const a of attempts) {
    const r = tryWrite(client, a.names, a.params, mcData, 'play')
    if (r.ok) return { ...r, uuid, url }
  }
  return { ok: false, url, uuid }
}

function sendKnownPacks (client, packs, mcData) {
  const state = 'configuration'
  return tryWrite(client, ['select_known_packs'], { knownPacks: packs, packs }, mcData, state)
}

function sendCustomPayload (client, channel, data, mcData, state) {
  const buf = Buffer.isBuffer(data) ? data : Buffer.from(data || [])
  return tryWrite(client, ['custom_payload', 'plugin_message'], {
    channel,
    data: buf
  }, mcData, state || 'play')
}

function sendOpenSign (client, mcData) {
  const pos = { x: 0, y: 64, z: 0 }
  tryWrite(client, ['open_sign_editor', 'open_sign'], {
    location: pos,
    pos,
    isFrontText: true
  }, mcData, 'play')
  return pos
}

module.exports = {
  decodeBrand,
  decodeRegister,
  chatComponent,
  translatableComponent,
  tryWrite,
  protocolSupportsKnownPacks,
  protocolHasTypedPayloads,
  protocolHasConfigPhase,
  sha1Hex,
  sendJoin,
  sendPosition,
  sendResourcePack,
  sendKnownPacks,
  sendCustomPayload,
  sendOpenSign,
  clientboundNames
}
