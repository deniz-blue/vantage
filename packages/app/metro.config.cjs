const { getDefaultConfig } = require("expo/metro-config");
const path = require("path");
const fs = require("fs");

/** @type {import("expo/metro-config").MetroConfig} */
const config = getDefaultConfig(__dirname);

config.resolver.unstable_enablePackageExports = true;

config.resolver.assetExts.push("wasm");
config.resolver.sourceExts.push("sql");

const CustomResolveMap = {};

const pnpmStore = path.resolve(__dirname, "../../node_modules/.pnpm");
const sqliteWasmPkgDir = fs.existsSync(pnpmStore)
	? fs.readdirSync(pnpmStore).find((d) => d.startsWith("@sqlite.org+sqlite-wasm@"))
	: undefined;

if (sqliteWasmPkgDir) {
	const sqliteWasmDist = path.join(
		pnpmStore,
		sqliteWasmPkgDir,
		"node_modules/@sqlite.org/sqlite-wasm/dist",
	);

	const workerFiles = ["sqlite3-worker1.mjs", "sqlite3-opfs-async-proxy.js"];

	for (let file of workerFiles) {
		CustomResolveMap[file] = {
			filePath: path.join(sqliteWasmDist, file),
			type: "sourceFile",
		};
	}

	const wasmPath = path.join(sqliteWasmDist, "sqlite3.wasm");
	const wasmContent = fs.readFileSync(wasmPath);

	config.server.enhanceMiddleware = (middleware) => {
		return (req, res, next) => {
			res.setHeader("Cross-Origin-Embedder-Policy", "credentialless");
			res.setHeader("Cross-Origin-Opener-Policy", "same-origin");

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

config.resolver.resolveRequest = (context, moduleName, platform) => {
	if (CustomResolveMap[moduleName]) return CustomResolveMap[moduleName];
	if (platform === "web" && moduleName.includes("Libraries/Utilities/codegenNativeComponent"))
		return { type: "sourceFile", filePath: require.resolve("./src/mocks/codegenNativeComponent") };
	return context.resolveRequest(context, moduleName, platform);
};

module.exports = config;
