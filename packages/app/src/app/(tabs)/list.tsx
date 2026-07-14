import { useCallback, useMemo, useState } from "react";
import { FlatList, ActivityIndicator, useWindowDimensions, LayoutChangeEvent } from "react-native";
import { useRouter } from "expo-router";
import { ListOptions, ResolvedEventContext, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Loader } from "../../components/base/Loader";
import { EmptyState } from "../../components/base/EmptyState";
import { EventCard } from "../../components/event/card/EventCard";
import { TextInput } from "../../components/base/input/TextInput";
import { Card } from "../../components/base/Card";
import { Colors } from "../../theme/colors";
import { Button } from "../../components/base/button/Button";
import { IconFilter, IconSearch } from "@tabler/icons-react-native";
import { IconSize, Radius } from "../../theme/sizing";
import { UseQueryResult } from "@tanstack/react-query";
import { Draft, produce } from "immer";
import { Spacing } from "../../theme/spacing";
import { Sheet } from "../../components/base/sheet/Sheet";
import { SegmentedControl } from "../../components/base/input/SegmentedControl";
import { BooleanControl } from "../../components/base/input/BooleanControl";
import { Text } from "../../components/base/Text";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";
import { RefreshControl } from "react-native-gesture-handler";

const PAGE_SIZE = 50;
const CARD_WIDTH = 200;

type ListItem = UseQueryResult<Vantage.ResolvedEvent> | null;

export const useListRenderItem = () => {
	const router = useRouter();
	return useCallback(
		({ item }: { item: ListItem }) => {
			if (item === null) return <Box flex={1} />;

			return (
				<ResolvedEventContext.Provider value={item.data ?? null}>
					<Box flex={1}>
						<EventCard
							onPress={item.data?.id ? () => router.push(`/event/${item.data!.id}`) : undefined}
						/>
					</Box>
				</ResolvedEventContext.Provider>
			);
		},
		[router],
	);
};

export default function List() {
	const { width } = useWindowDimensions();
	const insets = useSafeAreaInsets();
	const [filtersOpen, setFiltersOpen] = useState(false);
	const [headerHeight, setHeaderHeight] = useState(0);

	const [filters, setFilters] = useState<ListOptions>({
		orderBy: "instanceStart",
	});

	const numColumns = Math.round(width / CARD_WIDTH) || 1;

	const patchFilters = (recipe: (draft: Draft<ListOptions>) => void) =>
		setFilters((p) => produce(p, recipe));

	const {
		queries,
		fetchNextPage,
		rowsQuery: { refetch },
		isFetchingNextPage,
		isFetching,
	} = useEventListInfiniteQuery({
		pageSize: PAGE_SIZE,
		...filters,
	});

	const renderItem = useListRenderItem();

	const items: ListItem[] = [
		...queries,
		...(Array.from({ length: numColumns - (queries.length % numColumns) }).fill(
			null,
		) as ListItem[]),
	];

	return (
		<Box flex={1}>
			<Box component={SafeAreaView} absoluteFill style={{ zIndex: 1, pointerEvents: "box-none" }}>
				<ListHeader
					filters={filters}
					isFetching={isFetching}
					patchFilters={patchFilters}
					setFiltersOpen={setFiltersOpen}
					onLayout={(e) => setHeaderHeight(e.nativeEvent.layout.height)}
				/>
			</Box>

			<ListFiltersSheet
				open={filtersOpen}
				onClose={() => setFiltersOpen(false)}
				filters={filters}
				patchFilters={patchFilters}
			/>

			<FlatList
				style={{ flex: 1 }}
				contentContainerStyle={{
					paddingTop: insets.top + Spacing.sm * 2 + headerHeight,
					paddingBottom: insets.bottom + Spacing.sm,
					paddingHorizontal: Spacing.sm,
				}}
				data={items}
				renderItem={renderItem}
				keyExtractor={(item, idx) => item?.data?.id ?? idx.toString()}
				ListEmptyComponent={<EmptyState message="No events found" />}
				onEndReached={() => fetchNextPage()}
				refreshing={isFetching}
				refreshControl={
					<RefreshControl
						refreshing={isFetching}
						onRefresh={() => refetch()}
						style={{ top: insets.top + headerHeight }}
					/>
				}
				ItemSeparatorComponent={<Box pt="sm" />}
				columnWrapperStyle={
					numColumns > 1 ? { gap: Spacing.sm, justifyContent: "space-between" } : undefined
				}
				numColumns={numColumns}
				key={numColumns}
				ListFooterComponent={<ListFooter isFetchingNextPage={isFetchingNextPage} />}
			/>
		</Box>
	);
}

export const ListHeader = ({
	filters,
	isFetching,
	patchFilters,
	setFiltersOpen,
	onLayout,
}: {
	isFetching: boolean;
	filters: ListOptions;
	patchFilters: (recipe: (draft: Draft<ListOptions>) => void) => void;
	setFiltersOpen: (open: boolean) => void;
	onLayout: (e: LayoutChangeEvent) => void;
}) => {
	return (
		<Card radius={Radius.md} m="sm" p="sm" bg={Colors.Background} onLayout={onLayout}>
			<Box direction="row" gap="sm">
				<Box flex={1}>
					<TextInput
						placeholder="Search..."
						leftSection={<IconSearch size={IconSize.xs} color={Colors.Text} />}
						rightSection={isFetching && <ActivityIndicator />}
						value={filters.search ?? ""}
						onChangeText={(text) =>
							patchFilters((q) => {
								q.search = text;
							})
						}
					/>
				</Box>
				<Button
					rightSection={<IconFilter size={IconSize.xs} color={Colors.Text} />}
					onPress={() => setFiltersOpen(true)}
				>
					Filters
				</Button>
			</Box>
		</Card>
	);
};

export const ListFooter = ({ isFetchingNextPage }: { isFetchingNextPage: boolean }) => {
	return (
		<Box>
			{isFetchingNextPage && (
				<Box p="sm" align="center">
					<Loader />
				</Box>
			)}
			<Box h={200} />
		</Box>
	);
};

export const ListFiltersSheet = ({
	open,
	onClose,
	filters,
	patchFilters,
}: {
	open: boolean;
	onClose: () => void;
	filters: ListOptions;
	patchFilters: (recipe: (draft: Draft<ListOptions>) => void) => void;
}) => {
	const todayMs = useMemo(() => Temporal.Now.instant().epochMilliseconds, []);

	return (
		<Sheet open={open} onClose={onClose}>
			<Box gap="sm">
				<Box direction="row" gap="sm" align="center">
					<Box flex={1}>
						<Text>Filter by</Text>
					</Box>
					<SegmentedControl<"" | "future" | "past">
						value={filters.afterTimestamp ? "future" : filters.beforeTimestamp ? "past" : ""}
						onChange={(value) =>
							patchFilters((q) => {
								if (value === "future") {
									q.afterTimestamp = todayMs;
									q.beforeTimestamp = undefined;
								} else if (value === "past") {
									q.beforeTimestamp = todayMs;
									q.afterTimestamp = undefined;
								} else {
									q.beforeTimestamp = undefined;
									q.afterTimestamp = undefined;
								}
							})
						}
						options={[
							{ label: "Past", value: "past" },
							{ label: "All", value: "" },
							{ label: "Future", value: "future" },
						]}
					/>
				</Box>

				<Box direction="row" gap="sm" align="center">
					<Box flex={1}>
						<Text>Sort by</Text>
					</Box>
					<SegmentedControl<Exclude<ListOptions["orderBy"], undefined>>
						value={filters.orderBy ?? "instanceStart"}
						onChange={(value) =>
							patchFilters((q) => {
								q.orderBy = value;
							})
						}
						options={[
							{ label: "Date", value: "instanceStart" },
							{ label: "Name", value: "name" },
						]}
					/>
				</Box>

				<BooleanControl
					label="Has error"
					value={filters.error ?? null}
					onChange={(value) =>
						patchFilters((q) => {
							q.error = value ?? undefined;
						})
					}
				/>
			</Box>
		</Sheet>
	);
};
