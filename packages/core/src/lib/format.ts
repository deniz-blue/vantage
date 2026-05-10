import type { EventData } from "@evnt/schema";

export type EventParseResult = {
	parsed: EventData;
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

export const parseEventFormat = (raw: string, fmt: Vantage.EventFormat) => {
	const format = EventFormatRegistry.get(fmt.type);
	if (!format) throw new Error(`No event format defined for id: ${fmt.type}`);
	return format.parse(raw, fmt);
};
