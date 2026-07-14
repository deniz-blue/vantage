const DEV = process.env.APP_VARIANT === "development";

if (DEV) console.log("[app.config.js] Running in development mode");

export default {
	/** @type {import("expo/config").ExpoConfig} */
	expo: {
		name: DEV ? "Vantage (Development)" : "Vantage",
		slug: "vantage",
		version: "0.1.0",
		orientation: "portrait",
		icon: "./public/icon512-maskable.png",
		userInterfaceStyle: "dark",
		backgroundColor: "#242424",
		ios: {
			supportsTablet: true,
		},
		scheme: ["vantage", "evnt", "web+evnt"],
		experiments: {
			tsconfigPaths: true,
			typedRoutes: true,
		},
		android: {
			package: DEV ? "lt.tsx.vantage.dev" : "lt.tsx.vantage",
			predictiveBackGestureEnabled: false,
			versionCode: 2,
			intentFilters: [
				{
					action: "VIEW",
					data: [
						{
							scheme: "https",
							host: "vantage.tsx.lt",
						},
					],
					autoVerify: true,
					category: ["BROWSABLE", "DEFAULT"],
				},
				{
					action: "VIEW",
					data: [
						{
							scheme: "https",
							host: "eventsl.ink",
						},
					],
					autoVerify: true,
					category: ["BROWSABLE", "DEFAULT"],
				},
				{
					action: "VIEW",
					data: [
						{
							scheme: "vantage",
						},
					],
					category: ["BROWSABLE", "DEFAULT"],
				},
			],
		},
		web: {
			output: "single",
			bundler: "metro",
			favicon: "./public/icon.svg",
		},
		plugins: [
			"expo-router",
			"expo-status-bar",
			[
				"expo-font",
				{
					fonts: ["./assets/fonts/Lexend_400Regular.ttf"],
				},
			],
			"expo-splash-screen",
			"expo-localization",
			"react-native-map-link",
			"expo-sharing",
			"react-native-enriched-markdown",
			"expo-sqlite",
		],
		extra: {
			router: {},
		},
	},
};
