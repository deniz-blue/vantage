import { queryOptions, useQueries, useQuery } from "@tanstack/react-query";
import { fetchEventSource, parseEventFormat } from "@vantage/core";
import { db } from "./drizzle";
import { schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { ResolvedEvent } from "./resolved-event";
import { queryClient } from "../query-client";

export const eventQueryKey = (id: Vantage.EventId) => ["event", id] as const;

export const eventQueryFn = async (id: Vantage.EventId): Promise<ResolvedEvent> => {
	const { source, format } = await db
		.select()
		.from(schema.eventMeta)
		.where(eq(schema.eventMeta.id, id))
		.then(rows => rows[0]);

	if (!source || !format) throw new Error(`Event with id ${id} not found`);
	const resolveResult = await fetchEventSource(source);
	const parseResult = resolveResult.raw ? parseEventFormat(resolveResult.raw, format) : null;

	await db
		.update(schema.eventCache)
		.set({
			id,
			raw: resolveResult.raw,
			parsed: parseResult ? parseResult.parsed : null,
			error: resolveResult.error,
			revision: resolveResult.revision,
			updatedAt: new Date(),
		})
		.where(eq(schema.eventCache.id, id));

	return {
		id,
		data: parseResult ? parseResult.parsed : null,
		raw: resolveResult.raw,
		revision: resolveResult.revision,
		error: resolveResult.error,
		source,
		format,
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
