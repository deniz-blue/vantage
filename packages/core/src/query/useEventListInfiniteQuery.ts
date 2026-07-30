import { useInfiniteQuery, useQueries } from "@tanstack/react-query";
import { eventListQueryFn, eventListQueryKey, type ListOptions } from "./useEventListQuery";
import { eventQueryFn } from "./useEventQuery";
import { EventResolver } from "../lib/resolve";

const PAGE_SIZE = 20;

export interface InfiniteListOptions extends Omit<ListOptions, "limit" | "offset"> {
	pageSize?: number;
}

export const useEventListInfiniteQuery = ({
	pageSize = PAGE_SIZE,
	...options
}: InfiniteListOptions = {}) => {
	const rowsQuery = useInfiniteQuery({
		queryKey: [...eventListQueryKey(options), "infinite"],
		queryFn: async ({ pageParam = 0 }) => {
			return eventListQueryFn({
				...options,
				limit: pageSize,
				offset: pageParam,
			});
		},
		initialPageParam: 0,
		getNextPageParam: (lastPage, _allPages, lastPageParam) => {
			if (lastPage.length < pageSize) return undefined;
			return (lastPageParam as number) + pageSize;
		},
		staleTime: 5 * 1000 * 60,
		placeholderData: (data) => data,
	});

	const rows = rowsQuery.data?.pages.flat() ?? [];

	const queries = useQueries({
		queries: rows.map((row) => ({
			queryKey: ["event", row.event_meta.id, "from-list"],
			queryFn: async () => eventQueryFn(EventResolver.fromDatabase(EventResolver.new(), row)),
			staleTime: 5 * 1000 * 60,
		})),
	});

	const events = queries.reduce<Vantage.ResolvedEvent[]>(
		(acc, q) => (q.data ? [...acc, q.data] : acc),
		[],
	);

	return {
		rowsQuery,
		rows,
		queries,
		events,
		fetchNextPage: rowsQuery.fetchNextPage,
		hasNextPage: rowsQuery.hasNextPage,
		isFetchingNextPage: rowsQuery.isFetchingNextPage,
		isLoading: rowsQuery.isLoading,
		isFetching: rowsQuery.isFetching || queries.some((q) => q.isFetching),
	};
};
