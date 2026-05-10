import { EventData } from "@evnt/schema";
import { createContext, useContext } from "react";

export interface ResolvedEvent {
	id: Vantage.EventId | null;
	data: EventData | null;
	raw: string | null;
	error: Vantage.Error | null;
	revision: Vantage.Revision;
	source: Vantage.EventSource;
	format: Vantage.EventFormat;
}

export const ResolvedEventContext = createContext<ResolvedEvent | null>(null);
export const useResolvedEvent = (): ResolvedEvent => {
	const resolved = useContext(ResolvedEventContext);
	if (!resolved) return {
		id: null,
		data: null,
		raw: null,
		error: {
			kind: "unknown",
			message: "No event data available",
		},
		revision: {},
		source: { type: "unknown" } as Vantage.EventSource,
		format: { type: "unknown" } as Vantage.EventFormat,
	};
	return resolved;
};

