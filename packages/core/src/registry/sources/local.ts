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
	editable: true,
});
