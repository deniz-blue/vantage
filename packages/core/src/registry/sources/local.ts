import { EventsManager } from "../../database/event-manager";
import { defineEventSource } from "../../lib/source";

declare global {
	namespace Vantage {
		interface EventSourceMap {
			local: {
				type: "local";
			};
		}
	}
}

defineEventSource({
	type: "local",
	edit: async ({ id, data }) => {
		const raw = JSON.stringify(data);
		await EventsManager.updateEventCache(id, {
			raw,
			parsed: data,
			error: null,
		});
	},
});
