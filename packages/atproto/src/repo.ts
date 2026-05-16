import type { CanonicalResourceUri } from "@atcute/lexicons";
import { parseCanonicalResourceUri, type AtprotoDid, type Nsid } from "@atcute/lexicons/syntax";
import { getPdsEndpoint } from "@atcute/identity";
import { didDocumentResolver } from "./services";
import { Client, ok, simpleFetchHandler } from "@atcute/client";
import type { } from "@atcute/atproto";
import { useInfiniteQuery } from "@tanstack/react-query";

export const repoClient = async (repo: AtprotoDid) => {
	const didDocument = await didDocumentResolver.resolve(repo);
	const pds = getPdsEndpoint(didDocument) ?? "https://bsky.social";

	return new Client({
		handler: simpleFetchHandler({
			service: pds,
		}),
	});
};

export const repoGetRecordUri = async (aturi: CanonicalResourceUri) => {
	const { repo, collection, rkey } = parseCanonicalResourceUri(aturi);
	return await repoGetRecord(repo as AtprotoDid, collection, rkey);
};

export const repoGetRecord = async (repo: AtprotoDid, collection: Nsid, rkey: string) => {
	const rpc = await repoClient(repo);
	return await rpc.get("com.atproto.repo.getRecord", {
		params: {
			repo,
			collection,
			rkey,
		},
	});
};

export const repoListRecords = async (repo: AtprotoDid, collection: Nsid, opts?: { limit?: number; cursor?: string }) => {
	const rpc = await repoClient(repo);
	return await rpc.get("com.atproto.repo.listRecords", {
		params: {
			repo,
			collection,
			limit: opts?.limit,
			cursor: opts?.cursor,
		},
	});
};

export const useInfiniteRepoListRecords = (repo?: AtprotoDid | null, collection?: Nsid | null) => {
	return useInfiniteQuery({
		queryKey: ["repoListRecords", repo, collection],
		enabled: !!repo && !!collection,
		queryFn: async ({ pageParam }) => {
			if (!repo || !collection) throw new Error("Repo and collection are required");
			return ok(await repoListRecords(repo, collection, { limit: 50, cursor: pageParam }));
		},
		initialPageParam: undefined as string | undefined,
		getNextPageParam: (lastPage) => lastPage.cursor,
	});
};
