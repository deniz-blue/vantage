import { schema, db } from "@vantage/db";
import { and, eq } from "drizzle-orm";
import { createComputedData, eventQueryFnNoId, invalidateEventQuery } from "@vantage/core";
import { invalidateEventListQueries } from "@vantage/core";

export const dbShortcuts = {
	getFromMeta: (source: Vantage.EventSource, format: Vantage.EventFormat) => db
		.select()
		.from(schema.eventMeta)
		.where(and(eq(schema.eventMeta.source, source), eq(schema.eventMeta.format, format))),

	refetchEvent: async (id: Vantage.EventId) => {
		const result = await db
			.select()
			.from(schema.eventMeta)
			.leftJoin(schema.eventCache, eq(schema.eventMeta.id, schema.eventCache.id))
			.where(eq(schema.eventMeta.id, id))
			.then(rows => rows[0]);

		if (!result) return console.warn("Event not found for refetch", id);

		const {
			event_cache: cached,
			event_meta: { source, format },
		} = result;

		const resolved = await eventQueryFnNoId(source, format);

		const values: schema.EventCache = {
			id,
			error: resolved.error,
			parsed: resolved.data,
			raw: resolved.raw,
			revision: resolved.revision,
			updatedAt: Temporal.Now.instant(),
			computed: createComputedData(resolved.data),
		};

		await db
			.insert(schema.eventCache)
			.values(values)
			.onConflictDoUpdate({
				target: schema.eventCache.id,
				set: values,
			});

		invalidateEventQuery(id);
		invalidateEventListQueries();
	},
};
