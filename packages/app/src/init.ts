import { SplashScreen } from "expo-router";
SplashScreen.preventAutoHideAsync();

// == Polyfills ==
import "temporal-polyfill-lite/global";

// == Services ==
import "./lib/init-db";
import { initializeDatabase } from "@vantage/db";

// == Entry Point (loaded after migration completes) ==
initializeDatabase().then(() => {
	import("expo-router/entry");
});
