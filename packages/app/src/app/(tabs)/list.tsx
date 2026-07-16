import { useMemo, useState } from "react";
import { ActivityIndicator } from "react-native";
import { ListOptions, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Loader } from "../../components/base/Loader";
import { TextInput } from "../../components/base/input/TextInput";
import { Card } from "../../components/base/Card";
import { Colors } from "../../theme/colors";
import { Button } from "../../components/base/button/Button";
import { IconFilter, IconSearch } from "@tabler/icons-react-native";
import { IconSize, Radius } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";
import { Sheet } from "../../components/base/sheet/Sheet";
import { SegmentedControl } from "../../components/base/input/SegmentedControl";
import { BooleanControl } from "../../components/base/input/BooleanControl";
import { Text } from "../../components/base/Text";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import { EventList } from "../../components/event/list/EventList";

const PAGE_SIZE = 50;

export const useListFiltersStore = create<ListOptions>()(
	immer(() => ({
		limit: PAGE_SIZE,
		orderBy: "instanceStart",
	})),
);

export default function List() {
	const insets = useSafeAreaInsets();
	const filters = useListFiltersStore();
	const { queries, fetchNextPage, isFetching } = useEventListInfiniteQuery(filters);

	return (
		<Box flex={1}>
			<Box
				pos="absolute"
				top={insets.top + Spacing.sm}
				left={Spacing.sm}
				right={Spacing.sm}
				zIndex={1}
			>
				<ListHeader loading={isFetching} />
			</Box>

			<EventList
				contentContainerStyle={{
					paddingTop: insets.top + Spacing.sm * 2 + 48, // 48 = header height
					paddingBottom: insets.bottom + Spacing.sm,
					paddingHorizontal: Spacing.sm,
				}}
				queries={queries}
				onEndReached={() => fetchNextPage()}
			/>
		</Box>
	);
}

export const ListHeader = ({ loading }: { loading: boolean }) => {
	const [filtersOpen, setFiltersOpen] = useState(false);
	const filters = useListFiltersStore();

	return (
		<Card radius={Radius.md} p="sm" bg={Colors.Background} flex={1}>
			<Box direction="row" gap="sm">
				<Box flex={1}>
					<TextInput
						placeholder="Search..."
						leftSection={<IconSearch size={IconSize.xs} color={Colors.Text} />}
						rightSection={loading && <ActivityIndicator />}
						value={filters.search ?? ""}
						onChangeText={(text) =>
							useListFiltersStore.setState((q) => {
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

			<ListFiltersSheet open={filtersOpen} onClose={() => setFiltersOpen(false)} />
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

export const ListFiltersSheet = ({ open, onClose }: { open: boolean; onClose: () => void }) => {
	const filters = useListFiltersStore();
	const todayMs = useMemo(() => Temporal.Now.instant().epochMilliseconds, []);

	return (
		<Sheet open={open} onClose={onClose}>
			<Box gap="sm">
				<Box direction="row" gap="sm">
					<Box flex={1}>
						<Text>Filter by</Text>
					</Box>
					<SegmentedControl<"" | "future" | "past">
						value={filters.afterTimestamp ? "future" : filters.beforeTimestamp ? "past" : ""}
						onChange={(value) =>
							useListFiltersStore.setState((q) => {
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
							useListFiltersStore.setState((q) => {
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
						useListFiltersStore.setState((q) => {
							q.error = value ?? undefined;
						})
					}
				/>
			</Box>
		</Sheet>
	);
};
