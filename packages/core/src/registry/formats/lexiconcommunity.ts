import { parseResourceUri } from "@atcute/lexicons";
import { defineEventFormat } from "../../lib/format";
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
	parse: (raw, _, ctx) => {
		const json = JSON.parse(raw);
		const converted = convertFromLexicon(json, {
			did: ctx?.source?.type === "at" ? parseResourceUri(ctx.source.uri).repo : undefined,
		});
		return {
			parsed: converted,
			error: null,
		};
	}
});
