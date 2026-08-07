import type { OpenEvnt } from "@evnt/types";

export type EventParseResult = {
	parsed: OpenEvnt | null;
	error: Vantage.Error | null;
};

export type EventFormat<Format extends keyof Vantage.EventFormatMap> = {
	type: Format;
	parse: (ctx: {
		raw: string;
		format: Vantage.EventFormatMap[Format];
		source: Vantage.EventSource;
	}) => EventParseResult;
	inferFromRaw?: (ctx: {
		raw: string;
		source: Vantage.EventSource;
	}) => Vantage.EventFormatMap[Format] | null;
};

export const EventFormatRegistry = new Map<string, EventFormat<any>>();

export const defineEventFormat = <Type extends keyof Vantage.EventFormatMap>(
	fmt: EventFormat<Type>,
) => {
	EventFormatRegistry.set(fmt.type, fmt);
};

export const inferFormatFromRaw = (
	raw: string,
	source: Vantage.EventSource,
): Vantage.EventFormat | null => {
	for (const [, fmt] of EventFormatRegistry) {
		if (fmt.inferFromRaw) {
			const result = fmt.inferFromRaw({ raw, source });
			if (result) return result;
		}
	}
	return null;
};
