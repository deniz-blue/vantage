import { SplashScreen } from "expo-router";
SplashScreen.preventAutoHideAsync();

// == Polyfills ==
import "temporal-polyfill-lite/global";
import "@formatjs/intl-displaynames/polyfill";
import "@formatjs/intl-displaynames/locale-data/en";
import "@formatjs/intl-supportedvaluesof/polyfill";

// == Services ==
import "./lib/init-db";
import { initializeDatabase } from "@vantage/db";

// == Entry Point ==
initializeDatabase().then(() => {
	import("expo-router/entry");
});
