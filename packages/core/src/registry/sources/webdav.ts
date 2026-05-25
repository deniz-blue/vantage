import { defineEventSource } from "../../lib/source";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			webdav: {
				type: "webdav";
				url: string;
				path: string;
			};
		}

		interface Revision { }
	}
}

defineEventSource({
	type: "webdav",
	editable: true,

	resolve: async ({ url, path }) => {
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

export const webdav = {
	read: async (url: string, path: string) => {
		const res = await fetch(url);
		if (!res.ok) throw res;
		return await res.text();
	},

	write: async (url: string, path: string, content: string) => {
		const res = await fetch(url, {
			method: "PUT",
			body: content,
		});
		if (!res.ok) throw res;
	},

	list: async (url: string, path: string) => {
		const endpoint = new URL(path, url).href;
		const res = await fetch(endpoint, {
			method: "PROPFIND",
			headers: {
				Authorization: `Basic ${btoa("deniz:meow")}`,
			},
		});
		if (!res.ok) throw res;
		const text = await res.text();
		const parser = new DOMParser();
		const xml = parser.parseFromString(text, "application/xml");
		const items: {
			name: string;
			isDirectory: boolean;
			lastModified: string | undefined;
			etag: string | undefined;
		}[] = [];
		const responses = xml.getElementsByTagName("response");
		for (let i = 0; i < responses.length; i++) {
			const response = responses[i]!;
			const href = response.getElementsByTagName("href")[0]?.textContent;
			const displayname = response.getElementsByTagName("displayname")[0]?.textContent;
			const resourcetype = response.getElementsByTagName("resourcetype")[0];
			const lastmodified = response.getElementsByTagName("lastmodified")[0]?.textContent;
			const etag = response.getElementsByTagName("getetag")[0]?.textContent;
			if (href && displayname && resourcetype) {
				items.push({
					name: displayname,
					isDirectory: resourcetype.getElementsByTagName("collection").length > 0,
					lastModified: lastmodified,
					etag,
				});
			}
		}
		return items;
	},
};
