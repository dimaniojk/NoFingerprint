'use strict'

const fs = require('fs')
const path = require('path')
const mc = require('minecraft-protocol')
const { createProbeServer } = require('./server')
const { writeJson } = require('./report')
const { createPackHttp } = require('./httpPack')

function arg (name, fallback) {
  const i = process.argv.indexOf('--' + name)
  if (i === -1) return fallback
  const next = process.argv[i + 1]
  if (!next || next.startsWith('--')) return true
  return next
}

function repoRoot () {
  return path.resolve(__dirname, '../../..')
}

async function httpOnly () {
  const http = createPackHttp({ host: '127.0.0.1' })
  const snap = await http.listen()
  console.log('READY')
  console.log(`host=${snap.host}`)
  console.log(`port=${snap.port}`)
  console.log('minecraft_version=n/a')
  console.log('requested_client_config=http-only')
  console.log(`pack_http=${snap.baseUrl}`)
  console.log('HTTP-only mode: GET /test.zip /to-loopback-302 /to-loopback-307 /to-loopback-308 /chain')
  console.log('Ctrl+C to stop.')
}

async function main () {
  const version = arg('version', '1.20.1')
  const configuration = String(arg('config', 'default'))
  const host = arg('host', '127.0.0.1')
  const port = parseInt(arg('port', '25565'), 10)
  const timeoutMs = parseInt(arg('timeout-ms', '180000'), 10)
  const once = !!arg('once', false) || configuration === 'self-test'
  const doSelfTest = !!arg('self-test', false) || configuration === 'self-test'
  const doHttpOnly = !!arg('http-only', false)
  const writeReport = arg('write-report', once) !== 'false'
  const settleMs = parseInt(arg('settle-ms', '4000'), 10)
  const nfVersion = arg('nf-version', '1.1.7.1-nofingerprint.2-beta.1')

  if (doHttpOnly) {
    await httpOnly()
    return
  }

  const supported = mc.supportedVersions || []
  console.log(`minecraft-protocol ${require('minecraft-protocol/package.json').version}`)
  console.log(`supported_versions=${supported.join(', ')}`)

  if (version === '26.2' && !supported.includes('26.2')) {
    const msg = supported.includes('26.1')
      ? '26.2 is not in minecraft-protocol 1.68.0 (26.1 is). Architecture accepts 26.2; runtime is NOT_SUPPORTED until the library lists it.'
      : '26.2 is not in minecraft-protocol supportedVersions.'
    console.error('NOT_SUPPORTED ' + msg)
    const out = {
      nofingerprint_version: nfVersion,
      generated_at: new Date().toISOString(),
      runs: [{
        minecraft_version: '26.2',
        configuration,
        probes: [{
          minecraft_version: '26.2',
          configuration,
          probe: 'protocol_library',
          observed: supported,
          expected: '26.2 listed',
          result: 'NOT_SUPPORTED',
          notes: msg
        }]
      }]
    }
    const jsonPath = path.join(repoRoot(), 'runtime-probe-results.json')
    let merged = out
    if (fs.existsSync(jsonPath)) {
      try {
        const prev = JSON.parse(fs.readFileSync(jsonPath, 'utf8'))
        merged = { ...out, runs: (prev.runs || []).concat(out.runs) }
      } catch { /* replace */ }
    }
    fs.writeFileSync(jsonPath, JSON.stringify(merged, null, 2) + '\n')
    process.exit(2)
  }

  const effectiveConfig = doSelfTest ? 'self-test' : configuration
  const stamp = `${version}-${effectiveConfig}-${Date.now()}`
  const jsonlPath = path.join(__dirname, '..', 'results', stamp + '.jsonl')

  const { server, report, http } = createProbeServer({
    version,
    configuration: doSelfTest ? 'self-test' : configuration,
    host,
    port,
    once: once || doSelfTest,
    jsonlPath,
    nofingerprintVersion: nfVersion,
    settleMs
  })

  const httpSnap = await http.listen()

  server.on('error', (err) => {
    console.error('[listen]', err.message)
    process.exit(1)
  })

  if (!server.socketServer.listening) {
    await new Promise((resolve, reject) => {
      server.once('error', reject)
      server.once('listening', resolve)
    })
  }

  const addr = server.socketServer.address()
  console.log('READY')
  console.log(`host=${host}`)
  console.log(`port=${addr.port}`)
  console.log(`minecraft_version=${version}`)
  console.log(`requested_client_config=${doSelfTest ? 'self-test' : configuration}`)
  console.log(`pack_http=${httpSnap.baseUrl}`)
  console.log(`jsonl=${jsonlPath}`)
  console.log('Connect a local Minecraft client. Do not use public servers.')

  const jsonOut = path.join(repoRoot(), 'runtime-probe-results.json')

  let closed = false
  const closer = async (code) => {
    if (closed) return
    closed = true
    clearTimeout(timer)
    if (writeReport) {
      writeJson(jsonOut, report, true)
      console.log(`wrote ${jsonOut}`)
    }
    try { await http.close() } catch { /* ignore */ }
    try { server.close() } catch { /* ignore */ }
    process.exit(code)
  }

  const timer = setTimeout(() => {
    console.error(`timeout after ${timeoutMs}ms`)
    report.add({
      probe: 'timeout',
      observed: 'no complete session',
      expected: 'client connect',
      result: 'INCONCLUSIVE',
      notes: `No client finished the probe sequence within ${timeoutMs}ms.`
    })
    closer(3)
  }, timeoutMs)

  process.on('SIGINT', () => { closer(0) })

  if (doSelfTest) {
    const mcClient = require('minecraft-protocol')
    setTimeout(() => {
      const bot = mcClient.createClient({
        host,
        port: addr.port,
        username: 'HarnessBot',
        version,
        auth: 'offline',
        hideErrors: false,
        keepAlive: false
      })
      bot.on('error', (e) => console.error('[self-test bot]', e.message))
      bot.on('end', (r) => console.log('[self-test bot] end', r))
    }, 400)
  }

  server.on('close', () => {
    closer(0)
  })
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
