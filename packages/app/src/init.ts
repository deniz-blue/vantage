import "./polyfills-es2023";

console.log("Initializing app...");

import { SplashScreen } from "expo-router";
import { enableFreeze } from "react-native-screens";
SplashScreen.preventAutoHideAsync();
enableFreeze(true);

// == Polyfills ==
import "temporal-polyfill-lite/global";
import "@formatjs/intl-locale/polyfill.js";
import "@formatjs/intl-displaynames/polyfill.js";
import "@formatjs/intl-supportedvaluesof/polyfill.js";
import "@formatjs/intl-displaynames/locale-data/en.js";
import { CryptoKey, install as installQuickCrypto } from "react-native-quick-crypto";
installQuickCrypto();
// @ts-expect-error CryptoKey isn't declared on globalThis by quick-crypto
globalThis.CryptoKey = CryptoKey;

// == Services ==
import "./lib/init-db";

// == Entry Point ==
console.log("Starting app...");
import "expo-router/entry";
