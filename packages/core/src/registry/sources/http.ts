import { defineEventSource } from "../../lib/source";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			http: {
				type: "http";
				url: string;
			};
		}

		interface Revision {
			etag?: string;
			lastModifiedHeader?: string;
		}
	}
}

defineEventSource({
	type: "http",
	editable: false,

	shareLink: ({ url }) => `https://eventsl.ink/e?${new URLSearchParams({
		url,
	})}`,

	resolve: async ({ url }) => {
		const res = await fetch(url);
		if (!res.ok) throw res;
		const raw = await res.text();
		return {
			raw,
			error: null,
			revision: {
				etag: res.headers.get("ETag") ?? undefined,
				lastModifiedHeader: res.headers.get("Last-Modified") ?? undefined,
			},
		};
	},
});
