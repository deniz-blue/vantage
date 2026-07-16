import { useQueries, useQuery } from "@tanstack/react-query";
import { and, eq, inArray, SQL, sql } from "drizzle-orm";
import { db, schema } from "@vantage/db";
import { queryClient } from "./query-client";
import { eventQueryFn, eventQueryKey, eventQueryOptions } from "./useEventQuery";
import { EventResolver } from "../lib/resolve";

export interface ListOptions {
	orderBy?: "name" | "instanceStart" | "none";
	search?: string;
	error?: boolean;
	sourceType?: string;
	formatType?: string;
	beforeTimestamp?: number;
	afterTimestamp?: number;
	allTagIds?: string[];
	anyTagIds?: string[];
	limit?: number;
	offset?: number;
}

export interface ListQueryOptions extends ListOptions {
	enabled?: boolean;
}

export const eventListQueryKey = (options?: ListOptions) =>
	options ? (["list", options] as const) : (["list"] as const);

export const eventListQueryFn = async (
	options: ListOptions,
): Promise<
	{
		event_meta: schema.EventMeta;
		event_cache: schema.EventCache | null;
	}[]
> => {
	const sqlSearch = sql`EXISTS (SELECT 1 FROM json_each(${schema.eventCache.parsed}, '$.name') WHERE value LIKE ${"%" + options.search + "%"})`;
	const uniqueAllTagIds = [...new Set(options.allTagIds ?? [])];

	const allTagSearch =
		uniqueAllTagIds.length > 0
			? sql`EXISTS (
			SELECT 1
			FROM ${schema.eventTags}
			WHERE ${schema.eventTags.eventId} = ${schema.events.id}
				AND ${inArray(schema.eventTags.tagId, uniqueAllTagIds)}
			GROUP BY ${schema.eventTags.eventId}
			HAVING COUNT(DISTINCT ${schema.eventTags.tagId}) = ${uniqueAllTagIds.length}
		)`
			: undefined;

	const anyTagSearch = options.anyTagIds?.length
		? sql`EXISTS (
			SELECT 1
			FROM ${schema.eventTags}
			WHERE ${schema.eventTags.eventId} = ${schema.events.id}
				AND ${inArray(schema.eventTags.tagId, options.anyTagIds)}
		)`
		: undefined;

	const timeRangeSearch =
		options.beforeTimestamp || options.afterTimestamp
			? sql`EXISTS (SELECT 1 FROM json_each(${schema.eventCache.computed}, '$.timeRanges') WHERE 1=1
			${options.beforeTimestamp ? sql`AND json_extract(value, '$.high') < ${options.beforeTimestamp}` : sql``}
			${options.afterTimestamp ? sql`AND json_extract(value, '$.low') > ${options.afterTimestamp}` : sql``}
		)`
			: undefined;

	const where = and(
		(options.search?.length ?? 0) > 0 ? sqlSearch : undefined,
		options.error === true ? sql`${schema.eventCache.error} IS NOT NULL` : undefined,
		options.error === false ? sql`${schema.eventCache.error} IS NULL` : undefined,
		options.sourceType
			? sql`json_extract(${schema.eventMeta.source}, '$.type') = ${options.sourceType}`
			: undefined,
		options.formatType
			? sql`json_extract(${schema.eventMeta.format}, '$.type') = ${options.formatType}`
			: undefined,
		allTagSearch,
		anyTagSearch,
		timeRangeSearch,
	);

	let orderBy: SQL[] = [];

	if (options.orderBy === "name") {
		orderBy.push(sql`${schema.eventCache.parsed} ->> '$.name'`);
	} else if (options.orderBy === "instanceStart") {
		orderBy.push(
			sql`(SELECT max(json_extract(value, '$.low')) FROM json_each(${schema.eventCache.computed}, '$.timeRanges'))`,
		);
	}

	const rows = await db
		.select()
		.from(schema.events)
		.innerJoin(schema.eventMeta, eq(schema.events.id, schema.eventMeta.id))
		.leftJoin(schema.eventCache, eq(schema.events.id, schema.eventCache.id))
		.where(where)
		.orderBy(...orderBy)
		.limit(options.limit ?? 100)
		.offset(options.offset ?? 0);

	return rows;
};

export const useEventListQuery = ({ enabled, ...options }: ListQueryOptions) => {
	const rowsQuery = useQuery({
		queryKey: eventListQueryKey(options),
		placeholderData: (data) => data,
		staleTime: 5 * 1000 * 60, // 5 minutes
		enabled,
		queryFn: async () => eventListQueryFn(options),
	});

	const rows = rowsQuery.data ?? [];

	const events = useQueries({
		queries: rows.map((row) => ({
			...eventQueryOptions(row.event_meta.id),
			queryFn: async () => eventQueryFn(EventResolver.fromDatabase(EventResolver.new(), row)),
		})),
	});

	return {
		rowsQuery,
		events,
	};
};

export const invalidateEventListQueries = async () => {
	await queryClient.invalidateQueries({ queryKey: eventListQueryKey() });
};

export const invalidateEventQuery = async (id: Vantage.EventId) => {
	await queryClient.invalidateQueries({ queryKey: eventQueryKey(id) });
	await invalidateEventListQueries();
};
