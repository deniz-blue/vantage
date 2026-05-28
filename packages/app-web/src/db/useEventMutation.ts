import { schema, db } from "@vantage/db";
import { eq } from "drizzle-orm";
import { createComputedData, invalidateEventListQueries, invalidateEventQuery, mediawiki } from "@vantage/core";

export interface EventMutationParams {
	id: Vantage.EventId;
	raw: string;
	mediawikiComment?: string;
};

// for use with useMutation
export const eventMutationFn = async ({ id, raw, mediawikiComment }: EventMutationParams) => {
	const [{ source, format }] = await db
		.select()
		.from(schema.eventMeta)
		.where(eq(schema.eventMeta.id, id));

	if (!source || !format) throw new Error(`Event with id ${id} not found`);

	if (format.type !== "directory.evnt.event") throw new Error(`Only OpenEvnt format is supported`);

	// TODO: this is a hack and will break the moment we support editing non-OpenEvnt formats

	if (source.type === "local") {
		const updatedAt = Temporal.Now.instant();
		await db.update(schema.eventCache)
			.set({
				raw,
				parsed: JSON.parse(raw),
				updatedAt,
				computed: createComputedData(JSON.parse(raw)),
			})
			.where(eq(schema.eventCache.id, id));
	} else if (source.type === "mediawiki") {
		const cached = await db.select().from(schema.eventCache).where(eq(schema.eventCache.id, id)).get();
		const latest = cached?.revision.revisionId;
		const csrfToken = await mediawiki.getCsrfToken(source.url);
		await mediawiki.updatePage(source.url, source.title, {
			source: raw,
			comment: mediawikiComment ?? "Updated via Vantage",
			token: csrfToken,
			latest:  latest ? { id: latest } : undefined,
		});
	} else {
		throw new Error("Unsupported source");
	}

	invalidateEventQuery(id);
	invalidateEventListQueries();
};
