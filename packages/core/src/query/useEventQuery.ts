import { queryOptions, useQueries, useQuery } from "@tanstack/react-query";
import { queryClient } from "./query-client";
import { EventResolver, asyncPipe } from "../lib/resolve";

export const eventQueryKey = (id: Vantage.EventId) => ["event", id] as const;

export const eventQueryFnNoId = async (source: Vantage.EventSource, format: Vantage.EventFormat): Promise<Vantage.ResolvedEvent> => {
	return await asyncPipe(
		EventResolver.fetchIfNeeded.bind(EventResolver),
		EventResolver.parseIfNeeded.bind(EventResolver),
	)(EventResolver.new({ source, format }));
};

export const eventQueryFnDb = async (id: Vantage.EventId): Promise<Vantage.ResolvedEvent> => {
	let resolved = await EventResolver.selectFromDatabase(EventResolver.new({ id }));
	return await eventQueryFn(resolved);
};

export const eventQueryFn = async (/* mut */ resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> => {
	const mustFetch = EventResolver.mustFetch(resolved);
	const mustParse = EventResolver.mustParse(resolved);
	if (mustFetch) resolved = await EventResolver.fetch(resolved);
	if (mustParse) resolved = await EventResolver.parse(resolved);
	if ((mustFetch || mustParse) && resolved.source.type !== "local") await EventResolver.upsertToDatabase(resolved);
	return resolved;
};

export const eventQueryOptions = (id: Vantage.EventId) => {
	return queryOptions({
		queryKey: eventQueryKey(id),
		networkMode: "always",
		staleTime: 5 * 1000 * 60, // 5 minutes
		gcTime: 10 * 60 * 1000, // 10 minutes
		placeholderData: (data) => data,
		queryFn: async () => await eventQueryFnDb(id),
	});
};

export const useEventQuery = (id: Vantage.EventId) => useQuery(eventQueryOptions(id));
export type EventQuery = ReturnType<typeof useEventQuery>;

export const useEventQueries = (ids: Vantage.EventId[]) => useQueries({
	queries: ids.map(id => eventQueryOptions(id)),
});

// TODO(WebOnly)
// export const queryChangeBroadcastChannel = new BroadcastChannel("vantage:event-query-changes");
// queryChangeBroadcastChannel.onmessage = (event) => {
// 	const { id } = event.data as { id: Vantage.EventId };
// 	queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
// };

export const invalidateEventQuery = (id: Vantage.EventId) => {
	// queryChangeBroadcastChannel.postMessage({ id });
	queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
};
