// 同源静态资源服务与固定目标 API 代理，不接受调用者指定代理目标。
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.join(__dirname, 'dist');
const backend = new URL(process.env.BACKEND_URL || 'http://backend:8080');
const mime = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.png': 'image/png', '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.woff2': 'font/woff2', '.json': 'application/json' };
http.createServer((req, res) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  if (req.url.startsWith('/admin/api/')) {
    const proxy = http.request({ hostname: backend.hostname, port: backend.port, path: req.url, method: req.method, headers: { ...req.headers, host: backend.host }, timeout: 15000 }, upstream => {
      res.writeHead(upstream.statusCode, upstream.headers); upstream.pipe(res);
    });
    proxy.on('timeout', () => proxy.destroy());
    proxy.on('error', () => { if (!res.headersSent) res.writeHead(503, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, code: 'UPSTREAM_UNAVAILABLE', message: '后端服务暂不可用' })); });
    req.pipe(proxy); return;
  }
  if (req.method !== 'GET' && req.method !== 'HEAD') { res.writeHead(405); res.end(); return; }
  let pathname;
  try { pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname); } catch { res.writeHead(400); res.end(); return; }
  // 规范化后再次限定资源路径，防止静态目录穿越。
  let filename = path.resolve(root, '.' + pathname);
  if (!filename.startsWith(root + path.sep) && filename !== root) { res.writeHead(403); res.end(); return; }
  if (!fs.existsSync(filename) || fs.statSync(filename).isDirectory()) {
    if (path.extname(pathname)) { res.writeHead(404); res.end(); return; }
    filename = path.join(root, 'index.html');
  }
  res.setHeader('Content-Type', mime[path.extname(filename)] || 'application/octet-stream');
  res.setHeader('Cache-Control', filename.endsWith('index.html') ? 'no-cache' : 'public, max-age=3600');
  const stream = fs.createReadStream(filename);
  stream.on('error', () => { if (!res.headersSent) res.writeHead(404); res.end(); });
  if (req.method === 'HEAD') { res.end(); stream.destroy(); } else stream.pipe(res);
}).listen(8080, '0.0.0.0');
