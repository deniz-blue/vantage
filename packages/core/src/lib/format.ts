import type { EventData } from "@evnt/schema";
import { convertError } from "./source";

export type EventParseResult = {
	parsed: EventData | null;
	error: Vantage.Error | null;
};

export type EventParser<Format extends keyof Vantage.EventFormatMap> = (raw: string, fmt: Extract<Vantage.EventFormat, { type: Format }>) => EventParseResult;

export type EventFormat<Format extends keyof Vantage.EventFormatMap> = {
	type: Format;
	parse: EventParser<Format>;
};

export const EventFormatRegistry = new Map<string, EventFormat<any>>();

export const defineEventFormat = <Type extends keyof Vantage.EventFormatMap>(fmt: EventFormat<Type>) => {
	EventFormatRegistry.set(fmt.type, fmt);
};

export const parseEventFormat = (raw: string, fmt: Vantage.EventFormat): EventParseResult => {
	const format = EventFormatRegistry.get(fmt.type);

	if (!format) return {
		parsed: null,
		error: {
			kind: "unknown-format",
			message: `No parser defined for format type: ${fmt.type}`,
		},
	};

	try {
		return format.parse(raw, fmt);
	} catch (e) {
		return {
			parsed: null,
			error: convertError(e as any),
		};
	}
};
