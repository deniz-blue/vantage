
console.log("Initializing app...");

import { SplashScreen } from "expo-router";
import { enableFreeze } from "react-native-screens";
SplashScreen.preventAutoHideAsync();
enableFreeze(true);

// == Polyfills ==
import "./polyfills-es2023";
import "temporal-polyfill-lite/global";
import "@formatjs/intl-locale/polyfill.js";
import "@formatjs/intl-displaynames/polyfill.js";
import "@formatjs/intl-supportedvaluesof/polyfill.js";
import "@formatjs/intl-displaynames/locale-data/en.js";
import "./polyfills/platform";

// == Services ==
import "./lib/init-db";

// == Entry Point ==
console.log("Starting app...");
import "expo-router/entry";
