const DEV = process.env.APP_VARIANT === "development";

if (DEV) console.log("[app.config.js] Running in development mode");

const version = "0.1.8";
const versionCode = 8;

export default {
	/** @type {import("expo/config").ExpoConfig} */
	expo: {
		name: DEV ? "Vantage (Development)" : "Vantage",
		slug: "vantage",
		version,
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
			package: DEV ? "lt.tsx.dev.vantage" : "lt.tsx.vantage",
			versionCode,
			predictiveBackGestureEnabled: false,
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
				/** @type {import("./node_modules/expo-font/plugin/src/withFonts.ts").FontProps} */
				{
					fonts: ["./assets/fonts/Lexend_400Regular.ttf", "./assets/fonts/Lexend_700Bold.ttf"],
					android: {
						fonts: [
							{
								fontFamily: "Lexend",
								fontDefinitions: [
									{
										path: "./assets/fonts/Lexend_400Regular.ttf",
										weight: 400,
									},
									{
										path: "./assets/fonts/Lexend_700Bold.ttf",
										weight: 700,
									},
								],
							},
						],
					},
				},
			],
			"expo-splash-screen",
			"expo-localization",
			"react-native-map-link",
			"expo-sharing",
			"react-native-enriched-markdown",
			"expo-sqlite",
			"expo-image",
		],
		extra: {
			router: {},
		},
	},
};
