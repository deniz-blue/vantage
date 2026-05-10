import { defineEventFormat } from "../lib/format";
import { convertFromLexicon } from "@evnt/convert/community-lexicon";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"community.lexicon.calendar.event": {};
		}
	}
}

defineEventFormat({
	type: "community.lexicon.calendar.event",
	parse: (raw) => {
		const json = JSON.parse(raw);
		const converted = convertFromLexicon(json);
		return {
			parsed: converted,
			error: null,
		};
	}
});
