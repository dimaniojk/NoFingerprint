'use strict'

const http = require('http')
const { URL } = require('url')

/** Minimal zip: empty archive. Clients that GET this receive bytes, which is a failure for local-block tests. */
const EMPTY_ZIP = Buffer.from([
  0x50, 0x4b, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00
])

function createPackHttp (opts = {}) {
  const host = opts.host || '127.0.0.1'
  const connections = []
  const requests = []
  let tcpCount = 0

  const server = http.createServer((req, res) => {
    const rec = {
      t: Date.now(),
      method: req.method,
      url: req.url,
      remote: req.socket.remoteAddress,
      headers: { ...req.headers }
    }
    requests.push(rec)
    console.log(`[http] ${req.method} ${req.url} from ${req.socket.remoteAddress}`)

    const u = new URL(req.url, `http://${host}`)
    if (u.pathname === '/to-loopback-302') {
      res.writeHead(302, { Location: `http://127.0.0.1:9/private` })
      res.end()
      return
    }
    if (u.pathname === '/to-loopback-307') {
      res.writeHead(307, { Location: `http://127.0.0.1:9/private` })
      res.end()
      return
    }
    if (u.pathname === '/to-loopback-308') {
      res.writeHead(308, { Location: `http://127.0.0.1:9/private` })
      res.end()
      return
    }
    if (u.pathname === '/chain') {
      res.writeHead(302, { Location: `http://${host}:${server.address().port}/chain-308` })
      res.end()
      return
    }
    if (u.pathname === '/chain-308') {
      res.writeHead(308, { Location: `http://127.0.0.1:9/private` })
      res.end()
      return
    }
    if (u.pathname === '/private') {
      res.writeHead(200, { 'Content-Type': 'text/plain' })
      res.end('should-not-be-fetched')
      return
    }
    if (u.pathname === '/test.zip' || u.pathname === '/public.zip') {
      res.writeHead(200, {
        'Content-Type': 'application/zip',
        'Content-Length': EMPTY_ZIP.length
      })
      res.end(EMPTY_ZIP)
      return
    }
    res.writeHead(404)
    res.end('not found')
  })

  server.on('connection', (socket) => {
    tcpCount += 1
    connections.push({
      t: Date.now(),
      remote: socket.remoteAddress,
      remotePort: socket.remotePort
    })
  })

  function listen () {
    return new Promise((resolve, reject) => {
      server.once('error', reject)
      server.listen(0, host, () => {
        server.removeListener('error', reject)
        resolve(snapshot())
      })
    })
  }

  function snapshot () {
    const addr = server.address()
    return {
      host,
      port: addr && addr.port,
      baseUrl: addr ? `http://${host}:${addr.port}` : null,
      tcpCount,
      httpCount: requests.length,
      connections: connections.slice(),
      requests: requests.slice()
    }
  }

  function countsFor (pathname) {
    return {
      tcpCount,
      httpCount: requests.filter((r) => (r.url || '').split('?')[0] === pathname).length,
      requests: requests.filter((r) => (r.url || '').split('?')[0] === pathname)
    }
  }

  function close () {
    return new Promise((resolve) => server.close(() => resolve()))
  }

  return { server, listen, snapshot, countsFor, close, EMPTY_ZIP }
}

module.exports = { createPackHttp, EMPTY_ZIP }
