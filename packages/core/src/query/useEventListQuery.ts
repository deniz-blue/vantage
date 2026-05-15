import { useQuery } from "@tanstack/react-query";
import { and, eq, SQL, sql } from "drizzle-orm";
import { db, schema } from "@vantage/db";
import { queryClient } from "./query-client";

export interface ListOptions {
	orderBy?: "name" | "instanceStart" | "none";
	search?: string;
	error?: boolean;
	sourceType?: string;
	formatType?: string;
	beforeTimestamp?: number;
	afterTimestamp?: number;
	limit?: number;
	offset?: number;
}

export const eventListQueryKey = (options: ListOptions) => ["list", options] as const;

export const useEventListQuery = (options: ListOptions) => {
	return useQuery({
		queryKey: eventListQueryKey(options),
		placeholderData: data => data,
		staleTime: 5 * 1000 * 60, // 5 minutes
		queryFn: async () => {
			const sqlSearch = sql`EXISTS (SELECT 1 FROM json_each(${schema.eventCache.parsed}, '$.name') WHERE value LIKE ${"%"+options.search+"%"})`;

			const timeRangeSearch = (options.beforeTimestamp || options.afterTimestamp) ? sql`EXISTS (SELECT 1 FROM json_each(${schema.eventCache.computed}, '$.timeRanges') WHERE 1=1
				${options.beforeTimestamp ? sql`AND json_extract(value, '$.high') < ${options.beforeTimestamp}` : sql``}
				${options.afterTimestamp ? sql`AND json_extract(value, '$.low') > ${options.afterTimestamp}` : sql``}
			)` : undefined;

			const where = and(
				(options.search?.length ?? 0) > 0 ? sqlSearch : undefined,
				(options.error === true) ? sql`${schema.eventCache.error} IS NOT NULL` : undefined,
				(options.error === false) ? sql`${schema.eventCache.error} IS NULL` : undefined,
				(options.sourceType) ? sql`json_extract(${schema.eventMeta.source}, '$.type') = ${options.sourceType}` : undefined,
				(options.formatType) ? sql`json_extract(${schema.eventMeta.format}, '$.type') = ${options.formatType}` : undefined,
				timeRangeSearch,
			);

			let orderBy: SQL[] = [];

			if (options.orderBy === "name") {
				orderBy.push(sql`${schema.eventCache.parsed} ->> '$.name'`);
			} else if (options.orderBy === "instanceStart") {
				orderBy.push(sql`(SELECT max(json_extract(value, '$.low')) FROM json_each(${schema.eventCache.computed}, '$.timeRanges'))`);
			}

			return await db
				.select({ id: schema.events.id })
				.from(schema.events)
				.leftJoin(schema.eventCache, eq(schema.events.id, schema.eventCache.id))
				.leftJoin(schema.eventMeta, eq(schema.events.id, schema.eventMeta.id))
				.where(where)
				.orderBy(...orderBy)
				.limit(options.limit ?? 100)
				.offset(options.offset ?? 0)
				.then(rows => rows.map(r => r.id));
		},
	});
};

export const invalidateEventListQueries = () => {
	queryClient.invalidateQueries({ queryKey: ["list"] });
};
