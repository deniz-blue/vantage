import { useCallback, useMemo, useState } from "react";
import { FlatList, ActivityIndicator, useWindowDimensions } from "react-native";
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
import { IconSize } from "../../theme/sizing";
import { UseQueryResult } from "@tanstack/react-query";
import { Draft, produce } from "immer";
import { Spacing } from "../../theme/spacing";
import { Sheet } from "../../components/base/Sheet";
import { SegmentedControl } from "../../components/base/input/SegmentedControl";
import { BooleanControl } from "../../components/base/input/BooleanControl";
import { Text } from "../../components/base/Text";
import { SafeAreaView } from "react-native-safe-area-context";

const PAGE_SIZE = 50;
const CARD_WIDTH = 200;

export default function List() {
	const { width } = useWindowDimensions();
	const numColumns = Math.round(width / CARD_WIDTH) || 1;
	const router = useRouter();
	const [filtersOpen, setFiltersOpen] = useState(false);
	const todayMs = useMemo(() => Temporal.Now.instant().epochMilliseconds, []);
	const [filters, setFilters] = useState<ListOptions>({
		orderBy: "instanceStart",
	});

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

	const renderItem = useCallback(
		({ item }: { item: UseQueryResult<Vantage.ResolvedEvent> | null }) => {
			if (!item) return <Box flex={1} />;
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
		[router, numColumns],
	);

	const items = [
		...queries,
		...(Array.from({ length: numColumns - (queries.length % numColumns) }).fill(null) as null[]),
	];

	return (
		<Box component={SafeAreaView} flex={1}>
			<Box flex={1}>
				<Box gap="md">
					<Box pos="sticky" top={0} style={{ zIndex: 1 }}>
						<Card m="sm" p="sm" bg={Colors.Background}>
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
					</Box>

					<Sheet open={filtersOpen} onClose={() => setFiltersOpen(false)}>
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

					<Box px="md">
						<FlatList
							data={items}
							renderItem={renderItem}
							keyExtractor={(item, idx) => item?.data?.id ?? idx.toString()}
							ListEmptyComponent={<EmptyState message="No events found" />}
							onEndReached={() => fetchNextPage()}
							refreshing={isFetching}
							onRefresh={() => refetch()}
							ItemSeparatorComponent={<Box pt="sm" />}
							columnWrapperStyle={
								numColumns > 1 ? { gap: Spacing.sm, justifyContent: "space-between" } : undefined
							}
							numColumns={numColumns}
							key={numColumns}
						/>

						{isFetchingNextPage && (
							<Box p="md" align="center">
								<Loader size="small" />
							</Box>
						)}
					</Box>

					<Box h={200} />
				</Box>
			</Box>
		</Box>
	);
}
