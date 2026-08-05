import "./polyfills";

import {
	OAuthClient,
	type OAuthClientStores,
	type PublicClientMetadata,
	MemoryStore,
} from "@atcute/oauth-node-client";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { identityResolver } from "../services";
import { KVStore } from "./store";

export { type OAuthSession } from "@atcute/oauth-node-client";

declare const __DEV__: boolean;

export const PROD_CLIENT_ID = "https://vantage.tsx.lt/.well-known/oauth-client.json";
export const PROD_REDIRECT_URI = "https://vantage.tsx.lt/oauth/callback";
export const DEV_REDIRECT_URI = "http://127.0.0.1:3000/oauth/callback";

const redirectUri = __DEV__ ? DEV_REDIRECT_URI : PROD_REDIRECT_URI;
const metadata: PublicClientMetadata = {
	...(__DEV__ ? {} : { client_id: PROD_CLIENT_ID }),
	redirect_uris: [redirectUri],
	scope: [
		"atproto",
		"repo:directory.evnt.event",
		"repo:community.lexicon.calendar.rsvp",
		"repo:community.lexicon.calendar.event",
	].join(" "),
};

const stores: OAuthClientStores = {
	sessions: new KVStore("sessions", AsyncStorage),
	states: new KVStore("states", AsyncStorage),
	asMetadata: new MemoryStore({ maxSize: 50, ttl: 60e3, ttlAutopurge: true }),
	prMetadata: new MemoryStore({ maxSize: 50, ttl: 60e3, ttlAutopurge: true }),
	dpopNonces: new MemoryStore({ maxSize: 50, ttl: 60e3, ttlAutopurge: true }),
};

export const AtOAuthClient = new OAuthClient({
	metadata,
	stores,
	actorResolver: identityResolver,
});
