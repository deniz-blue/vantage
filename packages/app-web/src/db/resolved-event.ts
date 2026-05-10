import { EventData } from "@evnt/schema";
import { createContext, useContext } from "react";

export interface ResolvedEvent {
	id: Vantage.EventId;
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
		id: "00000000-0000-0000-0000-000000000000",
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

