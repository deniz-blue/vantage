import { defineEventSource } from "../../lib/source";
import { parseCanonicalResourceUri, type CanonicalResourceUri } from "@atcute/lexicons/syntax";
import {
	CompositeDidDocumentResolver,
	PlcDidDocumentResolver,
	WebDidDocumentResolver,
} from "@atcute/identity-resolver";
import type {} from "@atcute/atproto";
import { repoGetRecordUri, useAtAccounts, useAtClient } from "@vantage/atproto";
import { ok } from "@atcute/client";
import { EventsManager } from "../../database/event-manager";

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

	resolve: async ({ uri }) => {
		console.log(">>>", uri);
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

	shareLink: ({ uri }) => `https://eventsl.ink/e?at=${uri}`,

	edit: async ({ id, source, data }) => {
		const { collection, repo, rkey } = parseCanonicalResourceUri(source.uri);
		if (!useAtAccounts.getState().accounts[repo])
			throw new Error("No account found for DID: " + repo);
		if (useAtAccounts.getState().activeDid !== repo)
			await useAtClient.getState().signIn(repo as any);
		const { client } = useAtClient.getState();
		if (!client) throw new Error("No AT Protocol client available");
		const res = await client.post("com.atproto.repo.putRecord", {
			input: {
				collection,
				record: data as any,
				repo,
				rkey,
			},
		});
		if (!res.ok) throw new Error(res.data.error + ": " + res.data.message);
		const raw = JSON.stringify(data);
		await EventsManager.updateEventCache(id as any, {
			raw,
			parsed: data,
			error: null,
		});
	},
});
