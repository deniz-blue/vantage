import { defineEventFormat } from "../../lib/format";
import { icalendar } from "@evnt/convert";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"ics": {};
		}
	}
}

defineEventFormat({
	type: "ics",
	parse: (raw) => {
		try {
			if (!icalendar.from) throw new Error("ICS converter does not support `from`");
			const parsed = icalendar.from(raw);
			return { parsed, error: null };
		} catch (e: any) {
			return {
				parsed: null,
				error: { kind: "parse-error", message: e.message ?? "Failed to parse ICS" },
			};
		}
	}
});
