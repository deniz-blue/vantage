import { CryptoKey, install as installQuickCrypto } from "react-native-quick-crypto";
installQuickCrypto();
// @ts-expect-error CryptoKey isn't declared on globalThis by quick-crypto
globalThis.CryptoKey = CryptoKey;
