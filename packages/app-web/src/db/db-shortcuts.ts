import { schema } from "@vantage/db";
import { db } from "./drizzle";
import { eq } from "drizzle-orm";
import { invalidateEventQuery } from "./useEventQuery";
import { EventMeta } from "@vantage/db/src/schema";
import { parseEventFormat } from "@vantage/core";
import { invalidateEventListQueries } from "./useEventListQuery";

export const dbShortcuts = {
	getCachedEventRaw: (id: Vantage.EventId) => db
		.select()
		.from(schema.eventCache)
		.where(eq(schema.eventCache.id, id))
		.then(rows => rows[0]?.raw || null),

	getCachedParsed: (id: Vantage.EventId) => db
		.select()
		.from(schema.eventCache)
		.where(eq(schema.eventCache.id, id))
		.then(rows => rows[0]?.parsed || null),

	insertLocalEvent: async (raw: string, format: Vantage.EventFormat): Promise<Vantage.EventId> => {
		const source: Vantage.EventSource = { type: "local" };
		const id = await db.transaction(async tx => {
			const id = crypto.randomUUID();
			const now = new Date();
			await tx.insert(schema.events).values({
				id,
				updatedAt: now,
			});
			await tx.insert(schema.eventMeta).values({
				id,
				format,
				source,
				updatedAt: now,
			});

			const { error, parsed } = parseEventFormat(raw, format);

			await tx.insert(schema.eventCache).values({
				id,
				raw,
				parsed,
				error,
				revision: {},
				updatedAt: now,
			});
			return id;
		});
		invalidateEventListQueries();
		return id;
	},

	insertEventMeta: async ({ source, format }: Pick<EventMeta, "source" | "format">): Promise<Vantage.EventId> => {
		const id = await db.transaction(async tx => {
			const now = new Date();
			const id = crypto.randomUUID();
			await tx.insert(schema.events).values({
				id,
				updatedAt: now,
			});
			await tx.insert(schema.eventMeta).values({
				id,
				format,
				source,
				updatedAt: now,
			});
			return id;
		});
		invalidateEventListQueries();
		return id;
	},

	deleteEventMeta: async (id: Vantage.EventId): Promise<void> => {
		await db.transaction(async tx => {
			await tx.delete(schema.events).where(eq(schema.events.id, id));
			await tx.delete(schema.eventMeta).where(eq(schema.eventMeta.id, id));
			await tx.delete(schema.eventCache).where(eq(schema.eventCache.id, id));
		});
		invalidateEventQuery(id);
		invalidateEventListQueries();
	},

	deleteEventCache: async (id: Vantage.EventId): Promise<void> => {
		await db.delete(schema.eventCache).where(eq(schema.eventCache.id, id));
		invalidateEventQuery(id);
		invalidateEventListQueries();
	},
};
