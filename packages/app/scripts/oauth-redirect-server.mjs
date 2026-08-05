#!/usr/bin/env node
/**
 * AT Protocol OAuth loopback redirect server for development.
 *
 * Serves HTML with <script>window.location = ...</script> that redirects
 * to the app's custom scheme. This is more reliable than a 302 redirect
 * in Chrome Custom Tabs (which can drop query params during the redirect).
 *
 * External Android device: `adb reverse tcp:3000 tcp:3000` so the device's
 * 127.0.0.1:3000 reaches this host.
 */
import { createServer } from "node:http";

const PORT = 3000;
const HOST = "127.0.0.1";
const SCHEME = "vantage://oauth/callback";

const html = (location) => `<!DOCTYPE html>
<html>
<head><title>Redirecting…</title></head>
<body>
<script>window.location = ${JSON.stringify(location)};</script>
</body>
</html>`;

const server = createServer((req, res) => {
	const url = new URL(req.url, `http://${HOST}:${PORT}`);
	const target = new URL(SCHEME);
	url.searchParams.forEach((v, k) => target.searchParams.append(k, v));

	const location = target.toString();
	console.log(`→ ${location}`);

	res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
	res.end(html(location));
});

server.listen(PORT, HOST, () => {
	console.log(`Listening on http://${HOST}:${PORT}`);
});
