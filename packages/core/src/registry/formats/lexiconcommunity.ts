import { parseCanonicalResourceUri } from "@atcute/lexicons";
import { defineEventFormat } from "../../lib/format";
import { communityLexicon } from "@evnt/convert";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"community.lexicon.calendar.event": {};
		}
	}
}

defineEventFormat({
	type: "community.lexicon.calendar.event",
	parse: ({ raw, source }) => {
		try {
			if (!communityLexicon.from) throw new Error("Converter does not support `from`");
			const did = source.type === "at" ? parseCanonicalResourceUri(source.uri).repo : undefined;
			const parsed = communityLexicon.from(raw, { did });
			return { parsed, error: null };
		} catch (e: any) {
			return {
				parsed: null,
				error: { kind: "parse-error", message: e.message ?? "Failed to parse" },
			};
		}
	},
});
