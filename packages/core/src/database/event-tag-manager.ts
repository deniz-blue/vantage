import { db, schema } from "@vantage/db";
import { eq } from "drizzle-orm";

export const EventTagManager = new (class {
	async getTagsForEvent(eventId: Vantage.EventId): Promise<schema.Tag[]> {
		return await db
			.select()
			.from(schema.tags)
			.innerJoin(schema.eventTags, eq(schema.eventTags.tagId, schema.tags.id))
			.where(eq(schema.eventTags.eventId, eventId))
			.then((rows) => rows.map((row) => row.tags));
	}
})();
