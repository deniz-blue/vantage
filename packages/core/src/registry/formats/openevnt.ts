import { defineEventFormat } from "../../lib/format";
import { OpenEvntSchema, type OpenEvnt } from "@evnt/schema";

declare global {
	namespace Vantage {
		interface EventFormatMap {
			"directory.evnt.event": {};
		}
	}
}

defineEventFormat({
	type: "directory.evnt.event",
	parse: ({ raw }) => {
		const json = JSON.parse(raw);
		const parsed: OpenEvnt = OpenEvntSchema.parse(json);
		return {
			parsed,
			error: null,
		};
	},
	inferFromRaw: ({ raw }) => {
		try {
			const json = JSON.parse(raw);
			if (typeof json?.$type === "string") {
				if (json.$type === "directory.evnt.event") return { type: "directory.evnt.event" };
				if (json.$type === "community.lexicon.calendar.event")
					return { type: "community.lexicon.calendar.event" };
			}

			if (typeof json.v === "string" && json.v === "0.1" && typeof json.name === "object")
				return { type: "directory.evnt.event" };
		} catch {}
		return null;
	},
});
