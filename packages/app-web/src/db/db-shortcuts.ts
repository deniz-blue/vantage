import { schema, db } from "@vantage/db";
import { and, eq } from "drizzle-orm";
import { createComputedData, invalidateEventQuery } from "@vantage/core";
import { parseEventFormat } from "@vantage/core";
import { invalidateEventListQueries } from "@vantage/core";
import { sqlite } from "./drizzle";

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

	getFromMeta: (source: Vantage.EventSource, format: Vantage.EventFormat) => db
		.select()
		.from(schema.eventMeta)
		.where(and(eq(schema.eventMeta.source, source), eq(schema.eventMeta.format, format))),

	insertLocalEvent: async (raw: string, format: Vantage.EventFormat): Promise<Vantage.EventId> => {
		const source: Vantage.EventSource = { type: "local" };
		const id = await sqlite.transaction(async tx => {
			const id = crypto.randomUUID();
			const now = Temporal.Now.instant();
			await tx.query(db.insert(schema.events).values({
				id,
				updatedAt: now,
			}));
			await tx.query(db.insert(schema.eventMeta).values({
				id,
				format,
				source,
				updatedAt: now,
			}));

			const { error, parsed } = parseEventFormat(raw, format);

			await tx.query(db.insert(schema.eventCache).values({
				id,
				raw,
				parsed,
				error,
				revision: {},
				updatedAt: now,
				computed: createComputedData(parsed),
			}));
			return id;
		});
		invalidateEventListQueries();
		return id;
	},

	insertEventMeta: async ({ source, format }: { source: Vantage.EventSource; format: Vantage.EventFormat }): Promise<Vantage.EventId> => {
		const id = await sqlite.transaction(async tx => {
			const now = Temporal.Now.instant();
			const id = crypto.randomUUID();
			await tx.query(db.insert(schema.events).values({
				id,
				updatedAt: now,
			}));
			await tx.query(db.insert(schema.eventMeta).values({
				id,
				format,
				source,
				updatedAt: now,
			}));
			return id;
		});
		invalidateEventListQueries();
		return id;
	},

	deleteEventMeta: async (id: Vantage.EventId): Promise<void> => {
		await sqlite.transaction(async tx => {
			await tx.query(db.delete(schema.events).where(eq(schema.events.id, id)));
			await tx.query(db.delete(schema.eventMeta).where(eq(schema.eventMeta.id, id)));
			await tx.query(db.delete(schema.eventCache).where(eq(schema.eventCache.id, id)));
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
