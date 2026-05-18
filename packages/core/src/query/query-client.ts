import { QueryCache, QueryClient } from "@tanstack/react-query";

export const queryClient = new QueryClient({
	queryCache: new QueryCache({
		onError: (error, query) => {
			console.error("Query error:", { error, query });
		},
	}),
});

// @ts-ignore
globalThis.queryClient = queryClient;
