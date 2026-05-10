import { defineEventSource } from "./source";
import { defineEventFormat } from "./format";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"unknown": {};
		}

		interface EventSourceMap {
			"unknown": {};
		}
	}
}

defineEventFormat({
	type: "unknown",
	parse: () => { throw new Error("Unknown event format") },
});

defineEventSource({
	type: "unknown",
});
