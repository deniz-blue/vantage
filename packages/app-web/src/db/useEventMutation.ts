import { db } from "./drizzle";
import { schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { dbShortcuts } from "./db-shortcuts";

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

	await dbShortcuts.updateLocalEvent(id, raw);
};
