const { getDefaultConfig } = require("expo/metro-config");
/** @type {import("expo/metro-config").MetroConfig} */
const config = getDefaultConfig(__dirname);

config.resolver.assetExts.push("wasm");
config.resolver.sourceExts.push("sql");

config.server.enhanceMiddleware = (middleware) => {
	return (req, res, next) => {
		res.setHeader("Cross-Origin-Embedder-Policy", "credentialless");
		res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
		middleware(req, res, next);
	};
};


module.exports = config;
