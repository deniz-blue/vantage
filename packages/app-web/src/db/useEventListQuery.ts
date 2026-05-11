import { useQuery } from "@tanstack/react-query";
import { and, eq, sql } from "drizzle-orm";
import { db } from "./drizzle";
import { schema } from "@vantage/db";
import { queryClient } from "../query-client";

export interface ListOptions {
	search?: string;
	error?: boolean;
	sourceType?: string;
	formatType?: string;
}

export const eventListQueryKey = (options: ListOptions) => ["list", options] as const;

export const useEventListQuery = (options: ListOptions) => {
	return useQuery({
		queryKey: eventListQueryKey(options),
		placeholderData: data => data,
		staleTime: 5 * 1000 * 60, // 5 minutes
		queryFn: async () => {
			const sqlSearch = sql`EXISTS (SELECT 1 FROM jsonb_each_text(${schema.eventCache.parsed}->'name') AS name(lang, val) WHERE val ~* ${options.search})`;

			const where = and(
				(options.search?.length ?? 0) > 0 ? sqlSearch : undefined,
			);

			return await db
				.select({ id: schema.events.id })
				.from(schema.events)
				.leftJoin(schema.eventCache, eq(schema.events.id, schema.eventCache.id))
				.leftJoin(schema.eventMeta, eq(schema.events.id, schema.eventMeta.id))
				.where(where)
				.then(rows => rows.map(r => r.id));
		},
	});
};

export const invalidateEventListQueries = () => {
	queryClient.invalidateQueries({ queryKey: ["list"] });
};
