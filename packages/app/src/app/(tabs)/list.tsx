import { useCallback, useState } from "react";
import { ScrollView, FlatList, ActivityIndicator } from "react-native";
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

const PAGE_SIZE = 20;

export default function List() {
	const router = useRouter();
	const [filtersOpen, setFiltersOpen] = useState(false);
	const [query, setQuery] = useState<ListOptions>({
		orderBy: "instanceStart",
	});

	const patchQuery = (recipe: (draft: Draft<ListOptions>) => void) =>
		setQuery((p) => produce(p, recipe));

	const numColumns = 2;

	const {
		queries,
		fetchNextPage,
		rowsQuery: { refetch },
		isFetchingNextPage,
		isFetching,
	} = useEventListInfiniteQuery({
		pageSize: PAGE_SIZE,
		...query,
	});

	const renderItem = useCallback(
		({ item }: { item: UseQueryResult<Vantage.ResolvedEvent> }) => (
			<ResolvedEventContext.Provider value={item.data ?? null}>
				<EventCard
					onPress={item.data?.id ? () => router.push(`/event/${item.data!.id}`) : undefined}
				/>
			</ResolvedEventContext.Provider>
		),
		[router],
	);

	return (
		<Box flex={1}>
			<Box component={ScrollView} flex={1}>
				<Box gap="md">
					<Box pos="sticky" top={0} style={{ zIndex: 1 }}>
						<Card m="sm" p="sm" bg={Colors.Background}>
							<Box direction="row" gap="sm">
								<Box flex={1}>
									<TextInput
										placeholder="Search..."
										leftSection={<IconSearch size={IconSize.xs} />}
										rightSection={isFetching && <ActivityIndicator />}
										value={query.search ?? ""}
										onChangeText={(text) =>
											patchQuery((q) => {
												q.search = text;
											})
										}
									/>
								</Box>
								<Button
									rightSection={<IconFilter size={IconSize.xs} />}
									onPress={() => setFiltersOpen(true)}
								>
									Filters
								</Button>
							</Box>
						</Card>
					</Box>

					<Sheet open={filtersOpen} onClose={() => setFiltersOpen(false)}>
						<Box gap="sm">
							<EmptyState message="No filters available yet" />
						</Box>
					</Sheet>

					<Box px="md">
						<FlatList
							data={queries}
							renderItem={renderItem}
							keyExtractor={({ data }, idx) => data?.id ?? idx.toString()}
							ListEmptyComponent={<EmptyState message="No events found" />}
							onEndReached={() => fetchNextPage()}
							refreshing={isFetching}
							onRefresh={() => refetch()}
							ItemSeparatorComponent={<Box pt="sm" />}
							columnWrapperStyle={{ gap: Spacing.sm }}
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
