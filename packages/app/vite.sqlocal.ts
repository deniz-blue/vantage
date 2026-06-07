import type { Plugin, UserConfig } from "vite";

export const sqlocal = (): Plugin<UserConfig> => ({
	name: "vite-plugin-sqlocal",
	enforce: "pre",
	config(config: UserConfig): UserConfig {
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
			res.setHeader("Cross-Origin-Embedder-Policy", "credentialless");
			res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
			next();
		});
	},
});
