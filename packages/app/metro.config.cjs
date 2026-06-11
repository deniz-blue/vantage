const { getDefaultConfig } = require("expo/metro-config");
const path = require("path");
const fs = require("fs");

/** @type {import("expo/metro-config").MetroConfig} */
const config = getDefaultConfig(__dirname);

config.resolver.assetExts.push("wasm");
config.resolver.sourceExts.push("sql");

// === @sqlite.org/sqlite-wasm integration ===

const pnpmStore = path.resolve(__dirname, "../../node_modules/.pnpm");
const sqliteWasmPkgDir = fs.existsSync(pnpmStore)
	? fs.readdirSync(pnpmStore).find(d => d.startsWith("@sqlite.org+sqlite-wasm@"))
	: undefined;

if (sqliteWasmPkgDir) {
	const sqliteWasmDist = path.join(
		pnpmStore, sqliteWasmPkgDir,
		"node_modules/@sqlite.org/sqlite-wasm/dist",
	);

	// Resolve bare specifier Worker files that Metro can't resolve otherwise
	// (the package's exports field blocks accessing ./dist/* subpaths).
	const workerFiles = ["sqlite3-worker1.mjs", "sqlite3-opfs-async-proxy.js"];
	const workerMap = Object.fromEntries(
		workerFiles.map(f => [f, path.join(sqliteWasmDist, f)]),
	);

	config.resolver.resolveRequest = (context, moduleName, platform) => {
		if (workerMap[moduleName]) {
			return { filePath: workerMap[moduleName], type: "sourceFile" };
		}
		return context.resolveRequest(context, moduleName, platform);
	};

	// Preload sqlite3.wasm for the dev server middleware
	const wasmPath = path.join(sqliteWasmDist, "sqlite3.wasm");
	const wasmContent = fs.readFileSync(wasmPath);

	config.server.enhanceMiddleware = (middleware) => {
		return (req, res, next) => {
			res.setHeader("Cross-Origin-Embedder-Policy", "credentialless");
			res.setHeader("Cross-Origin-Opener-Policy", "same-origin");

			// sqlite-wasm resolves sqlite3.wasm relative to import.meta.url.
			// Metro's replacement of import.meta.url can yield null for dynamic
			// chunks — our pnpm patch adds `self.location.href` fallback, which
			// resolves to the page directory. Serve it wherever it's requested.
			if (req.url && /\/sqlite3\.wasm($|\?)/.test(req.url)) {
				res.setHeader("Content-Type", "application/wasm");
				res.setHeader("Content-Length", wasmContent.length);
				res.setHeader("Cache-Control", "public, immutable, max-age=31536000");
				res.end(wasmContent);
				return;
			}

			middleware(req, res, next);
		};
	};
}

module.exports = config;
