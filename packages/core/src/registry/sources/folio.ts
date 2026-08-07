import { defineEventSource, EventSourceRegistry, type EventSourceMeta } from "../../lib/source";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			folio: {
				type: "folio";
				id: string;
				baseUrl?: string;
				editToken?: string;
			};
		}
	}
}

export const FOLIO_BASE_URL = "https://folio.denizblue.workers.dev";

defineEventSource({
	type: "folio",

	resolve: async ({ baseUrl = FOLIO_BASE_URL, id }) => {
		const url = new URL(`/events/${id}`, baseUrl).toString();
		const http: EventSourceMeta<"http"> = EventSourceRegistry.get("http")!;
		return await http.resolve!({ type: "http", url });
	},

	shareLink: ({ baseUrl = FOLIO_BASE_URL, id }) =>
		`https://eventsl.ink/e?${new URLSearchParams({
			url: new URL(`/events/${id}`, baseUrl).toString(),
		})}`,

	edit: async ({ source, data }) => {
		if (!source.editToken) throw new Error("Edit token is required for editing Folio events.");
		const url = new URL(`/events/${source.id}`, source.baseUrl ?? FOLIO_BASE_URL).toString();
		const res = await fetch(url, {
			method: "PUT",
			headers: {
				"Content-Type": "application/json",
				Authorization: `Bearer ${source.editToken}`,
			},
			body: JSON.stringify(data),
		});
		if (!res.ok) throw new Error(`Failed to edit event: ${res.status} ${res.statusText}`);
	},
});
