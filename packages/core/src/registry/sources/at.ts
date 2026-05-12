import { defineEventSource } from "../../lib/source";
import { parseCanonicalResourceUri, type AtprotoDid } from "@atcute/lexicons/syntax";
import { CompositeDidDocumentResolver, PlcDidDocumentResolver, WebDidDocumentResolver } from "@atcute/identity-resolver";
import { Client } from "@atcute/client";
import { simpleFetchHandler } from "@atcute/client";
import { getPdsEndpoint } from "@atcute/identity";
import type { } from "@atcute/atproto";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			at: {
				type: "at";
				uri: string;
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
		const parsed = parseCanonicalResourceUri(uri);
		if (!parsed.ok) throw new Error(`Invalid at-uri: ${parsed.error}`);

		const didDocument = await didDocumentResolver.resolve(parsed.value.repo as AtprotoDid);
		const pds = getPdsEndpoint(didDocument) ?? "https://bsky.social";

		const rpc = new Client({
			handler: simpleFetchHandler({
				service: pds,
			}),
		});

		const res = await rpc.get("com.atproto.repo.getRecord", {
			params: {
				repo: parsed.value.repo,
				collection: parsed.value.collection,
				rkey: parsed.value.rkey,
			},
		});

		if (!res.ok) throw res;

		const raw = JSON.stringify(res.data.value);

		return {
			raw,
			error: null,
			revision: {
				cid: res.data.cid,
				etag: res.headers.get("ETag") ?? undefined,
				lastModifiedHeader: res.headers.get("Last-Modified") ?? undefined,
			},
		};
	},
});
