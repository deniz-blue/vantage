import { FlatList, ActivityIndicator, View, RefreshControl } from "react-native";
import { useRouter } from "expo-router";
import { ResolvedEventContext, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { EventCard } from "../../components/event/EventCard";
import { Colors } from "../../theme/colors";

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
		return (
			<Box flex={1} justify="center" align="center">
				<ActivityIndicator size="large" color={Colors.Primary} />
			</Box>
		);
	}

	return (
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
						<ActivityIndicator size="small" color={Colors.Primary} />
					</Box>
				) : null
			}
			ListEmptyComponent={
				<Box p="lg" align="center">
					<Text style={{ color: Colors.TextDimmed, fontSize: 16 }}>
						No events found
					</Text>
				</Box>
			}
			refreshControl={
				<RefreshControl
					refreshing={rowsQuery.isRefetching}
					onRefresh={() => rowsQuery.refetch()}
					tintColor={Colors.Primary}
				/>
			}
			contentContainerStyle={{ paddingBottom: 16, paddingHorizontal: 8 }}
			initialNumToRender={10}
			maxToRenderPerBatch={10}
			windowSize={5}
		/>
	);
}
