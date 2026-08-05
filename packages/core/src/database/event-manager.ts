import { db, schema } from "@vantage/db";
import { invalidateEventListQueries, invalidateEventQuery } from "../query/useEventListQuery";
import { eq } from "drizzle-orm";
import { createComputedData } from "./computed";
import { randomUUID } from "../utils/uuid";
import { asyncPipe, EventResolver } from "../lib/resolve";

export const EventsManager = new (class {
	async addEvent({
		format,
		source,
	}: Pick<schema.EventMeta, "format" | "source">): Promise<Vantage.EventId> {
		const id = await db.transaction(async (tx) => {
			const now = Temporal.Now.instant();
			const id = randomUUID();
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
		await invalidateEventListQueries();
		console.log("Added event with id", id);
		return id;
	}

	async addEventWithCache({
		format,
		source,
		raw,
		parsed,
		error,
	}: Pick<schema.EventMeta, "format" | "source"> &
		Pick<schema.EventCache, "raw" | "parsed" | "error">): Promise<Vantage.EventId> {
		console.log("Adding event with cache", { format, source, raw, parsed, error });
		const id = await db.transaction(async (tx) => {
			console.log("tx inner");
			const now = Temporal.Now.instant();
			console.log("tx vars", { now });
			const id = randomUUID();
			console.log("tx vars", { now, id });
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
		await invalidateEventListQueries();
		return id;
	}

	async removeEvent(id: Vantage.EventId): Promise<void> {
		await db.transaction(async (tx) => {
			await tx.delete(schema.events).where(eq(schema.events.id, id));
			await tx.delete(schema.eventMeta).where(eq(schema.eventMeta.id, id));
			await tx.delete(schema.eventCache).where(eq(schema.eventCache.id, id));
		});
		await invalidateEventListQueries();
	}

	async updateEventCache(
		id: Vantage.EventId,
		{ raw, parsed, error }: Pick<schema.EventCache, "raw" | "parsed" | "error">,
	): Promise<void> {
		const now = Temporal.Now.instant();
		await db
			.update(schema.eventCache)
			.set({
				raw,
				parsed,
				error,
				updatedAt: now,
				computed: createComputedData(parsed),
			})
			.where(eq(schema.eventCache.id, id));
		await invalidateEventListQueries();
	}

	async refetchEvent(id: Vantage.EventId): Promise<void> {
		await asyncPipe(
			EventResolver.selectFromDatabase,
			EventResolver.fetch,
			EventResolver.parse,
			EventResolver.upsertToDatabase,
		)(EventResolver.new({ id }));
		await invalidateEventQuery(id);
	}
})();
