import { useMemo, useRef } from "react";
import { ActivityIndicator } from "react-native";
import { ListOptions, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { TextInput } from "../../components/base/input/TextInput";
import { Card } from "../../components/base/Card";
import { Colors } from "../../theme/colors";
import { Button } from "../../components/base/button/Button";
import { IconFilter, IconSearch } from "@tabler/icons-react-native";
import { ControlHeight, IconSize, Radius } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";
import { Sheet, SheetRef } from "../../components/base/sheet/Sheet";
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

	const headerHeight = Spacing.sm * 2 + ControlHeight.md;
	const listTopPadding = insets.top + headerHeight + 2 * Spacing.sm;

	return (
		<Box flex={1}>
			<EventList
				contentContainerStyle={{
					paddingTop: listTopPadding,
					paddingBottom: insets.bottom + Spacing.sm,
					paddingHorizontal: Spacing.sm,
				}}
				queries={queries}
				onEndReached={() => fetchNextPage()}
			/>

			<Box
				style={{
					position: "absolute",
					top: insets.top,
					left: 0,
					right: 0,
					pointerEvents: "box-none",
				}}
				p="sm"
			>
				<ListHeader loading={isFetching} />
			</Box>
		</Box>
	);
}

export const ListHeader = ({ loading }: { loading: boolean }) => {
	const sheet = useRef<SheetRef>(null);
	const filters = useListFiltersStore();

	return (
		<Card radius={Radius.md} p="sm" bg={Colors.Background}>
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
					onPress={() => sheet.current?.present()}
				>
					Filters
				</Button>
			</Box>
			<Sheet ref={sheet}>
				<ListFiltersSheetContent />
			</Sheet>
		</Card>
	);
};

export const ListFiltersSheetContent = () => {
	const filters = useListFiltersStore();
	const todayMs = useMemo(() => Temporal.Now.instant().epochMilliseconds, []);

	return (
		<Box gap="md">
			<Box direction="row" gap="sm" align="center" justify="space-between">
				<Box>
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

			<Box direction="row" gap="sm" align="center" justify="space-between">
				<Box>
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
	);
};
