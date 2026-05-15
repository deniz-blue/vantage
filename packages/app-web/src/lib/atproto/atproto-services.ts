import type { } from "@atcute/atproto";
import { configureOAuth } from "@atcute/oauth-browser-client";
import { identityResolver } from "@vantage/atproto";

if (typeof window !== "undefined") {
	configureOAuth({
		metadata: {
			client_id: import.meta.env.VITE_OAUTH_CLIENT_ID,
			redirect_uri: import.meta.env.VITE_OAUTH_REDIRECT_URI,
		},
		identityResolver,
	});
}
