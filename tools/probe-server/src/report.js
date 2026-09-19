'use strict'

const fs = require('fs')
const path = require('path')

const RESULTS = ['PASS', 'FAIL', 'INCONCLUSIVE', 'NOT_SUPPORTED']

function nowIso () {
  return new Date().toISOString()
}

function elapsedMs (t0) {
  return Date.now() - t0
}

class Report {
  constructor (opts) {
    this.nofingerprintVersion = opts.nofingerprintVersion || '1.1.7.1-nofingerprint.2-beta.1'
    this.minecraftVersion = opts.minecraftVersion
    this.configuration = opts.configuration
    this.host = opts.host
    this.port = opts.port
    this.t0 = Date.now()
    this.probes = []
    this.events = []
    this.jsonlPath = opts.jsonlPath
    this.disconnects = []
    if (this.jsonlPath) {
      fs.mkdirSync(path.dirname(this.jsonlPath), { recursive: true })
      fs.writeFileSync(this.jsonlPath, '')
    }
  }

  event (name, detail) {
    const row = { t: elapsedMs(this.t0), name, ...detail }
    this.events.push(row)
    return row
  }

  add (probe) {
    const row = {
      minecraft_version: this.minecraftVersion,
      configuration: this.configuration,
      probe: probe.probe,
      observed: probe.observed ?? null,
      expected: probe.expected ?? null,
      result: RESULTS.includes(probe.result) ? probe.result : 'INCONCLUSIVE',
      notes: probe.notes || '',
      t_ms: elapsedMs(this.t0)
    }
    this.probes.push(row)
    if (this.jsonlPath) {
      fs.appendFileSync(this.jsonlPath, JSON.stringify(row) + '\n')
    }
    const mark = row.result === 'PASS' ? 'ok' : row.result === 'FAIL' ? 'FAIL' : row.result
    console.log(`[probe] ${row.probe}: ${mark} observed=${summarize(row.observed)} ${row.notes}`)
    return row
  }

  toJSON () {
    return {
      nofingerprint_version: this.nofingerprintVersion,
      generated_at: nowIso(),
      runs: [
        {
          minecraft_version: this.minecraftVersion,
          configuration: this.configuration,
          host: this.host,
          port: this.port,
          duration_ms: elapsedMs(this.t0),
          events: this.events,
          disconnects: this.disconnects,
          probes: this.probes
        }
      ]
    }
  }
}

function summarize (value) {
  if (value == null) return 'null'
  if (typeof value === 'string') return value.length > 80 ? value.slice(0, 77) + '...' : value
  try {
    const s = JSON.stringify(value)
    return s.length > 80 ? s.slice(0, 77) + '...' : s
  } catch {
    return String(value)
  }
}

function mergeRuns (existing, next) {
  if (!existing || !Array.isArray(existing.runs)) {
    return next
  }
  return {
    nofingerprint_version: next.nofingerprint_version,
    generated_at: next.generated_at,
    runs: existing.runs.concat(next.runs)
  }
}

function writeJson (file, report, merge) {
  let payload = report.toJSON()
  if (merge && fs.existsSync(file)) {
    try {
      payload = mergeRuns(JSON.parse(fs.readFileSync(file, 'utf8')), payload)
    } catch {
      /* replace */
    }
  }
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, JSON.stringify(payload, null, 2) + '\n')
  return payload
}

module.exports = { Report, writeJson, RESULTS }
