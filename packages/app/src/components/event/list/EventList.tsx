import { UseQueryResult } from "@tanstack/react-query";
import { ResolvedEventContext } from "@vantage/core";
import { useRouter } from "expo-router";
import { useCallback, useState } from "react";
import { Box } from "../../base/Box";
import { EventCard } from "../card/EventCard";
import { FlatList } from "react-native-gesture-handler";
import { useWindowDimensions, ViewStyle } from "react-native";
import { EmptyState } from "../../base/EmptyState";
import { EventActionsSheet } from "../../app/EventActionsSheet";

type EventQuery = UseQueryResult<Vantage.ResolvedEvent>;
export type EventListItem = EventQuery;

export const EventList = ({
	queries,
	contentContainerStyle,
	onEndReached,
}: {
	queries: EventQuery[];
	contentContainerStyle?: ViewStyle;
	onEndReached?: () => void;
}) => {
	const router = useRouter();
	const [showSheetFor, setShowSheetFor] = useState<string | null>(null);
	const resolvedForSheet = queries.find((q) => q.data?.id === showSheetFor)?.data ?? null;

	const { width } = useWindowDimensions();
	const CARD_WIDTH = 200;
	const numColumns = Math.round(width / CARD_WIDTH) || 1;

	const renderItem = useCallback(
		({ item }: { item: EventListItem }) => {
			const onPress = item.data?.id ? () => router.push(`/event/${item.data!.id}`) : undefined;
			const onLongPress = item.data?.id ? () => setShowSheetFor(item.data!.id) : undefined;

			return (
				<ResolvedEventContext value={item.data ?? null}>
					<Box flex={1} p="xs">
						<EventCard onPress={onPress} onLongPress={onLongPress} fill />
					</Box>
				</ResolvedEventContext>
			);
		},
		[router, setShowSheetFor],
	);

	const items = queries.map((query) => query);

	return (
		<Box flex={1}>
			<FlatList
				style={{ flex: 1 }}
				contentContainerStyle={contentContainerStyle}
				data={items}
				renderItem={renderItem}
				keyExtractor={(item, idx) => item.data?.id || idx.toString()}
				ListEmptyComponent={<EmptyState message="No events found" />}
				onEndReached={onEndReached}
				numColumns={numColumns}
				key={numColumns}
				ListFooterComponent={<Box h={200} />}
			/>

			<ResolvedEventContext value={resolvedForSheet}>
				<EventActionsSheet open={!!resolvedForSheet} onClose={() => setShowSheetFor(null)} />
			</ResolvedEventContext>
		</Box>
	);
};
