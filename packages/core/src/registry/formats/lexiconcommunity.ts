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
	parse: (raw, _fmt, ctx) => {
		try {
			if (!communityLexicon.from) throw new Error("Converter does not support `from`");
			const did = ctx?.source?.type === "at" ? (ctx.source as any).did : undefined;
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
