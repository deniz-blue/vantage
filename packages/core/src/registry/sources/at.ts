import { defineEventSource } from "../../lib/source";
import { type CanonicalResourceUri } from "@atcute/lexicons/syntax";
import { CompositeDidDocumentResolver, PlcDidDocumentResolver, WebDidDocumentResolver } from "@atcute/identity-resolver";
import type { } from "@atcute/atproto";
import { repoGetRecordUri } from "@vantage/atproto";
import { ok } from "@atcute/client";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			at: {
				type: "at";
				uri: CanonicalResourceUri;
			};
		}

		interface Revision {
			cid?: string;
		}
	}
}

export const didDocumentResolver = new CompositeDidDocumentResolver({
	methods: {
		plc: new PlcDidDocumentResolver(),
		web: new WebDidDocumentResolver(),
	},
});

defineEventSource({
	type: "at",
	editable: false,

	shareLink: ({ uri }) => `https://eventsl.ink/e?at=${uri}`,

	resolve: async ({ uri }) => {
		const res = await repoGetRecordUri(uri);
		const data = ok(res);
		const raw = JSON.stringify(data.value);

		return {
			raw,
			error: null,
			revision: {
				cid: data.cid,
				etag: res.headers.get("ETag") ?? undefined,
				lastModifiedHeader: res.headers.get("Last-Modified") ?? undefined,
			},
		};
	},
});
