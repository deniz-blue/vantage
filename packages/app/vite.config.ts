import { defineConfig, Plugin, UserConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";
import { AtprotoOAuth } from "@deniz-blue/vite-plugins";
import { PWAManifest } from "./vite.pwa";
import { rnw } from "vite-plugin-rnw";

const SERVER_HOST = "127.0.0.1";
const SERVER_PORT = 5173;

const sqlocal = (): Plugin<UserConfig> => ({
	name: "vite-plugin-sqlocal",
	enforce: "pre",
	config(config): UserConfig {
		return {
			optimizeDeps: {
				...config.optimizeDeps,
				exclude: [
					...(config.optimizeDeps?.exclude ?? []),
					"sqlocal",
					"@sqlite.org/sqlite-wasm",
				],
			},
			worker: {
				...config.worker,
				format: "es",
			},
		};
	},
	configureServer(server): void {
		server.middlewares.use((_, res, next) => {
			res.setHeader('Cross-Origin-Embedder-Policy', 'credentialless');
			res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
			next();
		});
	},
});

export default defineConfig({
	clearScreen: false,
	server: {
		host: SERVER_HOST,
		port: SERVER_PORT,
		forwardConsole: true,
	},

	resolve: {
		tsconfigPaths: true,
	},

	build: {
		sourcemap: true,
	},

	worker: {
		format: "es",
	},

	plugins: [
		sqlocal(),
		rnw(),
		AtprotoOAuth(),
		VitePWA({
			registerType: "prompt",
			injectRegister: "auto",
			manifest: PWAManifest,
			workbox: {
				globPatterns: ["**/*.{js,css,html,ico,png,svg}"],
				navigateFallbackDenylist: [/^\/embed/],
			},
		}),
	],
});
