import { queryOptions, useQueries, useQuery } from "@tanstack/react-query";
import { schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { queryClient } from "./query-client";
import { db } from "@vantage/db";
import { createComputedData } from "../database/computed";
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

export const eventQueryFnOld = async (id: Vantage.EventId): Promise<Vantage.ResolvedEvent> => {
	const result = await db
		.select()
		.from(schema.eventMeta)
		.leftJoin(schema.eventCache, eq(schema.eventMeta.id, schema.eventCache.id))
		.where(eq(schema.eventMeta.id, id))
		.then(rows => rows[0]);

	// hack
	if (!result) return {
		id: null,
		data: null,
		raw: null,
		error: { kind: "db", message: "Event not found", status: 404 },
		source: { type: "unknown" },
		format: { type: "unknown" },
		revision: {},
	};

	const {
		event_cache: cached,
		event_meta: { source, format },
	} = result;

	if (cached) {
		const now = Temporal.Now.instant();
		const maxAge = Temporal.Duration.from({ minutes: 5 });

		if (true) return {
			id,
			data: cached?.parsed || null,
			raw: cached?.raw || null,
			error: cached?.error || null,
			revision: cached?.revision || {},
			source,
			format,
		};
	};

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
		queryFn: async () => await eventQueryFnDb(id),
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
