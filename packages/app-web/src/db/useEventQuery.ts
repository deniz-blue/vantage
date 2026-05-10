import { queryOptions, useQueries, useQuery } from "@tanstack/react-query";
import { fetchEventSource, parseEventFormat } from "@vantage/core";
import { db } from "./drizzle";
import { schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { ResolvedEvent } from "./resolved-event";
import { queryClient } from "../query-client";

export const eventQueryKey = (id: Vantage.EventId) => ["event", id] as const;

export const eventQueryFnNoId = async (source: Vantage.EventSource, format: Vantage.EventFormat): Promise<ResolvedEvent> => {
	const resolveResult = await fetchEventSource(source);
	const parseResult = resolveResult.raw ? parseEventFormat(resolveResult.raw, format) : null;

	return {
		id: null,
		data: parseResult ? parseResult.parsed : null,
		raw: resolveResult.raw,
		revision: resolveResult.revision,
		error: resolveResult.error,
		source,
		format,
	};
};

export const eventQueryFn = async (id: Vantage.EventId): Promise<ResolvedEvent> => {
	const { source, format } = await db
		.select()
		.from(schema.eventMeta)
		.where(eq(schema.eventMeta.id, id))
		.then(rows => rows[0]);

	if (!source || !format) throw new Error(`Event with id ${id} not found`);
	const resolved = await eventQueryFnNoId(source, format);

	await db
		.update(schema.eventCache)
		.set({
			id,
			error: resolved.error,
			parsed: resolved.data,
			raw: resolved.raw,
			revision: resolved.revision,
			updatedAt: new Date(),
		})
		.where(eq(schema.eventCache.id, id));

	return {
		...resolved,
		id,
	};
};

export const eventQueryOptions = (id: Vantage.EventId) => {
	return queryOptions({
		queryKey: eventQueryKey(id),
		networkMode: "always",
		queryFn: async () => await eventQueryFn(id),
	});
};

export const useEventQuery = (id: Vantage.EventId) => useQuery(eventQueryOptions(id));
export type EventQuery = ReturnType<typeof useEventQuery>;

export const useEventQueries = (ids: Vantage.EventId[]) => useQueries({
	queries: ids.map(id => eventQueryOptions(id)),
});

export const queryChangeBroadcastChannel = new BroadcastChannel("vantage:event-query-changes");
queryChangeBroadcastChannel.onmessage = (event) => {
	const { id } = event.data as { id: Vantage.EventId };
	queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
};
export const invalidateEventQuery = (id: Vantage.EventId) => {
	queryChangeBroadcastChannel.postMessage({ id });
	queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
};
