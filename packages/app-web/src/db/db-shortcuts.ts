import { schema } from "@vantage/db";
import { db } from "./drizzle";
import { eq } from "drizzle-orm";
import { invalidateEventQuery } from "./useEventQuery";

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
		const id = crypto.randomUUID();
		const updatedAt = new Date();
		const source: Vantage.EventSource = { type: "local" };
		await db.transaction(async tx => {
			await tx.insert(schema.eventMeta).values({
				id,
				format,
				source,
				updatedAt,
			});
			await tx.insert(schema.eventCache).values({
				id,
				raw,
				revision: {},
				updatedAt,
			});
		});
		return id;
	},

	updateLocalEvent: async (id: Vantage.EventId, raw: string): Promise<void> => {
		const updatedAt = new Date();
		await db.update(schema.eventCache)
			.set({ raw, updatedAt })
			.where(eq(schema.eventCache.id, id));
		invalidateEventQuery(id);
	},
};
