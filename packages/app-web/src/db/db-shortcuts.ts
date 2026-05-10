import { schema } from "@vantage/db";
import { db } from "./drizzle";
import { eq } from "drizzle-orm";
import { invalidateEventQuery } from "./useEventQuery";
import { EventMeta } from "@vantage/db/src/schema";

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
		return await db.transaction(async tx => {
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
			await tx.insert(schema.eventCache).values({
				id,
				raw,
				revision: {},
				updatedAt: now,
			});
			return id;
		});
	},

	insertEventMeta: async ({ source, format }: Pick<EventMeta, "source" | "format">): Promise<Vantage.EventId> => {
		return await db.transaction(async tx => {
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
	},

	updateLocalEvent: async (id: Vantage.EventId, raw: string): Promise<void> => {
		const updatedAt = new Date();
		await db.update(schema.eventCache)
			.set({ raw, updatedAt })
			.where(eq(schema.eventCache.id, id));
		invalidateEventQuery(id);
	},
};
