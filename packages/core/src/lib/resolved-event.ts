import type { EventData } from "@evnt/schema";
import { createContext, useContext } from "react";

declare global {
	namespace Vantage {
		interface ResolvedEvent {
			id: Vantage.EventId | null;
			data: EventData | null;
			raw: string | null;
			error: Vantage.Error | null;
			revision: Vantage.Revision;
			source: Vantage.EventSource;
			format: Vantage.EventFormat;
		}
	}
}

export const ResolvedEventContext = createContext<Vantage.ResolvedEvent | null>(null);
export const useResolvedEvent = (): Vantage.ResolvedEvent => {
	const resolved = useContext(ResolvedEventContext);
	if (!resolved) return {
		id: null,
		data: null,
		raw: null,
		error: null,
		revision: {},
		source: { type: "unknown" } as Vantage.EventSource,
		format: { type: "unknown" } as Vantage.EventFormat,
	};
	return resolved;
};

