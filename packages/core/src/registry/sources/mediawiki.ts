import { defineEventSource } from "../../lib/source";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			mediawiki: {
				type: "mediawiki";
				/** URL of the MediaWiki instance */
				url: string;
				/** Title of the page to fetch */
				title: string;
			};
		}

		interface Revision {
			revisionId?: number;
			lastUpdatedMs?: number;
		}
	}
}

export namespace MediaWiki {
	export interface PageRevision {
		id: number;
		timestamp?: string;
	}

	export interface Page {
		id: number;
		key: string;
		title: string;
		latest: PageRevision;
		content_model: string;
		license?: {};
		source: string;
	};

	export interface SearchResult {
		id: number;
		key: string;
		title: string;
	};

	export interface CreateBody {
		source: string;
		title: string;
		comment: string | null;
		content_model?: string;
		token?: string;
	};

	export interface UpdateBody {
		source: string;
		comment: string | null;
		latest?: PageRevision;
	};
};

defineEventSource({
	type: "mediawiki",
	network: true,

	shareLink: ({ url, title }) => {
		const endpoint = new URL(`rest.php/v1/page/${title}`, url);
		return `https://eventsl.ink/e?${new URLSearchParams({
			url: endpoint.toString(),
		})}`;
	},

	resolve: async ({ url, title }) => {
		const endpoint = new URL(`rest.php/v1/page/${title}`, url);
		const res = await fetch(endpoint);
		if (!res.ok) throw res;

		const data = await res.json() as MediaWiki.Page;

		return {
			raw: data.source,
			revision: {
				lastUpdatedMs: data.latest.timestamp ? new Date(data.latest.timestamp).getTime() : undefined,
				revisionId: data.latest.id,
			},
			error: null,
		};
	},
});

export const mediawiki = {
	getPage: async (url: string, title: string): Promise<MediaWiki.Page> => {
		const endpoint = new URL(`rest.php/v1/page/${title}`, url);
		const res = await fetch(endpoint);
		if (!res.ok) throw res;
		const data = await res.json() as MediaWiki.Page;
		return data;
	},

	searchPages: async (url: string, query: string, limit: number = 10): Promise<string[]> => {
		const endpoint = new URL(`rest.php/v1/search/page?${new URLSearchParams([
			["q", query],
			["limit", limit.toString()],
		])}`, url);
		const res = await fetch(endpoint);
		if (!res.ok) throw res;
		const json = await res.json() as {
			pages: MediaWiki.SearchResult[];
		};
		return json.pages.map(page => page.key);
	},

	createPage: async (url: string, data: MediaWiki.CreateBody): Promise<MediaWiki.Page> => {
		const endpoint = new URL(`rest.php/v1/page`, url);
		const res = await fetch(endpoint, {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(data),
		});
		if (!res.ok) throw res;
		return await res.json() as MediaWiki.Page;
	},

	updatePage: async (url: string, title: string, data: MediaWiki.UpdateBody): Promise<MediaWiki.Page> => {
		const endpoint = new URL(`rest.php/v1/page/${title}`, url);
		const res = await fetch(endpoint, {
			method: "PUT",
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(data),
		});
		if (!res.ok) throw res;
		return await res.json() as MediaWiki.Page;
	},
};
