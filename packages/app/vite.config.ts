import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";
import { AtprotoOAuth } from "@deniz-blue/vite-plugins";
import { PWAManifest } from "./vite.pwa";
import { sqlocal } from "./vite.sqlocal";

/*
 Tried both vite-plugin-react-native-web and vite-plugin-rnw
 Both are giving issues
 I hope this issue is reproducible, and also goodluck

 PS: If vite dev server does start, it'll bind on 127.0.0.1 instead of 0.0.0.0 (atproto requirement)
*/

/// Uncomment for repro of your choice
const rnw = () => ({ name: "rnw" });
// import rnw from "vite-plugin-react-native-web";
// import { rnw } from "vite-plugin-rnw";

export default defineConfig({
	clearScreen: false,
	resolve: { tsconfigPaths: true },
	build: { sourcemap: true },
	server: { forwardConsole: true },

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
