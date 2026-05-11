import { queryOptions, useQueries, useQuery } from "@tanstack/react-query";
import { schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { queryClient } from "./query-client";
import { fetchEventSource } from "../lib/source";
import { parseEventFormat } from "../lib/format";
import { db } from "@vantage/db";
import { createComputedData } from "../database/computed";

export const eventQueryKey = (id: Vantage.EventId) => ["event", id] as const;

export const eventQueryFnNoId = async (source: Vantage.EventSource, format: Vantage.EventFormat): Promise<Vantage.ResolvedEvent> => {
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

export const eventQueryFn = async (id: Vantage.EventId): Promise<Vantage.ResolvedEvent> => {
	const {
		event_cache: cached,
		event_meta: { source, format },
	} = await db
		.select()
		.from(schema.eventMeta)
		.leftJoin(schema.eventCache, eq(schema.eventMeta.id, schema.eventCache.id))
		.where(eq(schema.eventMeta.id, id))
		.then(rows => rows[0]!);

	if (cached) return {
		id,
		data: cached?.parsed || null,
		raw: cached?.raw || null,
		error: cached?.error || null,
		revision: cached?.revision || {},
		source,
		format,
	};

	if (!source || !format) throw new Error(`Event with id ${id} not found`);
	const resolved = await eventQueryFnNoId(source, format);

	if (source.type !== "local") {
		const values: schema.EventCache = {
			id,
			error: resolved.error,
			parsed: resolved.data,
			raw: resolved.raw,
			revision: resolved.revision,
			updatedAt: Temporal.Now.instant(),
			computed: createComputedData(resolved.data),
		};

		// This can continue in the background maybe
		await db
			.insert(schema.eventCache)
			.values(values)
			.onConflictDoUpdate({
				target: schema.eventCache.id,
				set: values,
			});
	};

	return {
		...resolved,
		id,
	};
};

export const eventQueryOptions = (id: Vantage.EventId) => {
	return queryOptions({
		queryKey: eventQueryKey(id),
		networkMode: "always",
		staleTime: 5 * 1000 * 60, // 5 minutes
		gcTime: 10 * 60 * 1000, // 10 minutes
		placeholderData: (data) => data,
		queryFn: async () => await eventQueryFn(id),
	});
};

export const useEventQuery = (id: Vantage.EventId) => useQuery(eventQueryOptions(id));
export type EventQuery = ReturnType<typeof useEventQuery>;

export const useEventQueries = (ids: Vantage.EventId[]) => useQueries({
	queries: ids.map(id => eventQueryOptions(id)),
});

// TODO(WebOnly)
export const queryChangeBroadcastChannel = new BroadcastChannel("vantage:event-query-changes");
queryChangeBroadcastChannel.onmessage = (event) => {
	const { id } = event.data as { id: Vantage.EventId };
	queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
};

export const invalidateEventQuery = (id: Vantage.EventId) => {
	queryChangeBroadcastChannel.postMessage({ id });
	queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
};
