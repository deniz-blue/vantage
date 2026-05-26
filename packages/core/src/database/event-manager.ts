import { db, schema } from "@vantage/db";
import { invalidateEventListQueries } from "../query/useEventListQuery";
import { eq } from "drizzle-orm";
import { createComputedData } from "./computed";

export const EventsManager = new class {
	async addEvent({
		format,
		source,
	}: Pick<schema.EventMeta, "format" | "source">): Promise<Vantage.EventId> {
		const id = await db.transaction(async tx => {
			const now = Temporal.Now.instant();
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
	};

	async addEventWithCache({
		format,
		source,
		raw,
		parsed,
		error,
	}: Pick<schema.EventMeta, "format" | "source"> & Pick<schema.EventCache, "raw" | "parsed" | "error">): Promise<Vantage.EventId> {
		const id = await db.transaction(async tx => {
			const now = Temporal.Now.instant();
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
			await tx.insert(schema.eventCache).values({
				id,
				raw,
				parsed,
				error,
				revision: {},
				updatedAt: now,
				computed: createComputedData(parsed),
			});
			return id;
		});
		invalidateEventListQueries();
		return id;
	};
	
	async removeEvent(id: Vantage.EventId): Promise<void> {
		await db.transaction(async tx => {
			await tx.delete(schema.events).where(eq(schema.events.id, id));
			await tx.delete(schema.eventMeta).where(eq(schema.eventMeta.id, id));
			await tx.delete(schema.eventCache).where(eq(schema.eventCache.id, id));
		});
		invalidateEventListQueries();
	};
};

