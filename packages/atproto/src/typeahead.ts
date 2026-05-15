import { Client, simpleFetchHandler } from "@atcute/client";
import type { } from "@atcute/bluesky";

export const TYPEAHEAD_SERVICE_URL = "https://typeahead.waow.tech";

export const searchActorsTypeahead = async (q: string, opts?: { limit?: number }) => {
	const rpc = new Client({
		handler: simpleFetchHandler({
			service: TYPEAHEAD_SERVICE_URL,
		}),
	});

	return await rpc.get("app.bsky.actor.searchActorsTypeahead", {
		params: {
			q,
			limit: opts?.limit,
		},
	});
};
