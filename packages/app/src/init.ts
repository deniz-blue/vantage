import { SplashScreen } from "expo-router";
SplashScreen.preventAutoHideAsync();

// == Polyfills ==
import "temporal-polyfill-lite/global";

// == Services ==
import "./lib/init-db";

// == Entry Point ==
import "expo-router/entry";
