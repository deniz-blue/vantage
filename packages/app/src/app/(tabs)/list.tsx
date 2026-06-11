import { FlatList, ActivityIndicator, View, RefreshControl } from "react-native";
import { ResolvedEventContext, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { EventCard } from "../../components/event/EventCard";
import { Colors } from "../../theme/colors";

const PAGE_SIZE = 20;

export default function List() {
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
			<Box style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
				<ActivityIndicator size="large" color={Colors.Primary} />
			</Box>
		);
	}

	return (
		<FlatList
			data={events}
			keyExtractor={(_item, index) => String(index)}
			renderItem={({ item }) => (
				<ResolvedEventContext value={item.data ?? null}>
					<EventCard />
				</ResolvedEventContext>
			)}
			onEndReached={handleEndReached}
			onEndReachedThreshold={0.3}
			ListHeaderComponent={
				<View style={{ paddingHorizontal: 8, paddingTop: 8, paddingBottom: 4 }}>
					<Text style={{ fontSize: 24, fontWeight: "bold" }}>
						Events
					</Text>
					<Text style={{ fontSize: 13, color: Colors.TextDimmed }}>
						{events.length} event{events.length !== 1 ? "s" : ""}
					</Text>
				</View>
			}
			ListFooterComponent={
				isFetchingNextPage ? (
					<View style={{ padding: 16, alignItems: "center" }}>
						<ActivityIndicator size="small" color={Colors.Primary} />
					</View>
				) : null
			}
			ListEmptyComponent={
				<View style={{ padding: 32, alignItems: "center" }}>
					<Text style={{ color: Colors.TextDimmed, fontSize: 16 }}>
						No events found
					</Text>
				</View>
			}
			refreshControl={
				<RefreshControl
					refreshing={rowsQuery.isRefetching}
					onRefresh={() => rowsQuery.refetch()}
					tintColor={Colors.Primary}
				/>
			}
			contentContainerStyle={{ paddingBottom: 16 }}
			initialNumToRender={10}
			maxToRenderPerBatch={10}
			windowSize={5}
		/>
	);
}
