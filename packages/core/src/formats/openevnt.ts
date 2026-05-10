import { defineEventFormat } from "../lib/format";
import { EventDataSchema } from "@evnt/schema";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"directory.evnt.event": {};
		}
	}
}

defineEventFormat({
	type: "directory.evnt.event",
	parse: (raw) => {
		const json = JSON.parse(raw);
		const parsed = EventDataSchema.parse(json);
		return { parsed };
	}
});
