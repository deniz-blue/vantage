import { db } from "./drizzle";
import { schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { invalidateEventQuery } from "./useEventQuery";

export interface EventMutationParams {
	id: Vantage.EventId;
	raw: string;
};

// for use with useMutation
export const eventMutationFn = async ({ id, raw }: EventMutationParams) => {
	const [{ source, format }] = await db
		.select()
		.from(schema.eventMeta)
		.where(eq(schema.eventMeta.id, id));

	if (!source || !format) throw new Error(`Event with id ${id} not found`);

	if (source.type !== "local") throw new Error(`Only local events can be edited`);
	if (format.type !== "directory.evnt.event") throw new Error(`Only OpenEvnt format is supported`);

	// TODO: this is a hack and will break the moment we support editing non-OpenEvnt formats

	const updatedAt = new Date();
	await db.update(schema.eventCache)
		.set({ raw, parsed: JSON.parse(raw), updatedAt })
		.where(eq(schema.eventCache.id, id));
	invalidateEventQuery(id);
};
