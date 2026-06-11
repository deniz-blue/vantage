import { FlatList, RefreshControl } from "react-native";
import { useRouter } from "expo-router";
import { ResolvedEventContext, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Loader } from "../../components/base/Loader";
import { EmptyState } from "../../components/base/EmptyState";
import { Fab } from "../../components/base/Fab";
import { EventCard } from "../../components/event/EventCard";
import { Colors } from "../../theme/colors";

const FAB_SIZE = 56;

const PAGE_SIZE = 20;

export default function List() {
	const router = useRouter();
	const {
		events,
		fetchNextPage,
		hasNextPage,
		isFetchingNextPage,
		isLoading,
		rowsQuery,
	} = useEventListInfiniteQuery({
		pageSize: PAGE_SIZE,
		orderBy: "name",
	});

	const handleEndReached = () => {
		if (hasNextPage && !isFetchingNextPage) {
			fetchNextPage();
		}
	};

	if (isLoading) {
		return <EmptyState message="Loading events…" />;
	}

	return (
		<Box flex={1}>
			<FlatList
				data={events}
				keyExtractor={(_item, index) => String(index)}
				renderItem={({ item }) => {
					const eventId = item.data?.id;
					return (
						<ResolvedEventContext.Provider value={item.data ?? null}>
							<EventCard
								onPress={eventId ? () => router.push(`/event/${eventId}`) : undefined}
							/>
						</ResolvedEventContext.Provider>
					);
				}}
				onEndReached={handleEndReached}
				onEndReachedThreshold={0.3}
				ListHeaderComponent={
					<Box px="sm" pt="sm" pb={4}>
						<Text style={{ fontSize: 24, fontWeight: "bold" }}>
							Events
						</Text>
						<Text style={{ fontSize: 13, color: Colors.TextDimmed }}>
							{events.length} event{events.length !== 1 ? "s" : ""}
						</Text>
					</Box>
				}
				ListFooterComponent={
					isFetchingNextPage ? (
						<Box p="md" align="center">
							<Loader size="small" />
						</Box>
					) : null
				}
				ListEmptyComponent={
					<EmptyState loading={false} message="No events found" />
				}
				refreshControl={
					<RefreshControl
						refreshing={rowsQuery.isRefetching}
						onRefresh={() => rowsQuery.refetch()}
						tintColor={Colors.Primary}
					/>
				}
				contentContainerStyle={{ paddingBottom: 16 + FAB_SIZE + 16, paddingHorizontal: 8 }}
				initialNumToRender={10}
				maxToRenderPerBatch={10}
				windowSize={5}
			/>

			<Fab onPress={() => router.push("/new")} />
		</Box>
	);
}
