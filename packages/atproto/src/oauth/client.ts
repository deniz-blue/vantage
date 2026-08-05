import "./polyfills";

import {
	OAuthClient,
	type OAuthClientStores,
	MemoryStore,
} from "@atcute/oauth-node-client";
import { identityResolver } from "../services";
import { KVStore, type KVStorage } from "./store";

export { type OAuthSession } from "@atcute/oauth-node-client";

export const CLIENT_ID = "https://vantage.tsx.lt/.well-known/oauth-client.json";
export const REDIRECT_URI = "vantage://oauth/callback";

export function createOAuthClient(storage: KVStorage) {
	const stores: OAuthClientStores = {
		sessions: new KVStore("sessions", storage),
		states: new KVStore("states", storage),
		asMetadata: new MemoryStore({ maxSize: 50, ttl: 60e3, ttlAutopurge: true }),
		prMetadata: new MemoryStore({ maxSize: 50, ttl: 60e3, ttlAutopurge: true }),
		dpopNonces: new MemoryStore({ maxSize: 50, ttl: 60e3, ttlAutopurge: true }),
	};

	return new OAuthClient({
		metadata: {
			client_id: CLIENT_ID,
			redirect_uris: [REDIRECT_URI],
			scope: "atproto general",
		},
		stores,
		actorResolver: identityResolver,
	});
}
