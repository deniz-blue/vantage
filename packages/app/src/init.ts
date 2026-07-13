console.log("Initializing app...");

import { SplashScreen } from "expo-router";
SplashScreen.preventAutoHideAsync();

// == Polyfills ==
import "temporal-polyfill-lite/global";
import "@formatjs/intl-locale/polyfill.js";
import "@formatjs/intl-displaynames/polyfill.js";
import "@formatjs/intl-supportedvaluesof/polyfill.js";
import "@formatjs/intl-displaynames/locale-data/en.js";
import { polyfillWebCrypto } from "expo-standard-web-crypto";
polyfillWebCrypto();

// == Services ==
import "./lib/init-db";

// == Entry Point ==
console.log("Starting app...");
import "expo-router/entry";
