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

		interface PageDetails {
			id: number;
			key: string;
			title: string;
			latest: {
				id: number;
				timestamp: string;
			};
			content_model: string;
			license?: {};
			source: string;
		}

		const data = await res.json() as PageDetails;

		return {
			raw: data.source,
			revision: {
				lastUpdatedMs: new Date(data.latest.timestamp).getTime(),
				revisionId: data.latest.id,
			},
			error: null,
		};
	},
});

export const mediawiki = {
	searchPages: async (url: string, query: string, limit: number = 10): Promise<string[]> => {
		const endpoint = new URL(`rest.php/v1/search/page?${new URLSearchParams([
			["q", query],
			["limit", limit.toString()],
		])}`, url);
		const res = await fetch(endpoint);
		if (!res.ok) throw res;
		const json = await res.json() as {
			pages: {
				id: number;
				key: string;
				title: string;
			}[];
		};
		return json.pages.map(page => page.key);
	},
};
