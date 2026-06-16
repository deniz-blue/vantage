import { useCallback, useRef } from "react";
import { ScrollView, type NativeScrollEvent, type NativeSyntheticEvent, useWindowDimensions } from "react-native";
import { useRouter } from "expo-router";
import { ResolvedEventContext, useEventListInfiniteQuery } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Loader } from "../../components/base/Loader";
import { EmptyState } from "../../components/base/EmptyState";
import { Fab } from "../../components/base/Fab";
import { EventCard } from "../../components/event/card/EventCard";
import { Colors } from "../../theme/colors";
import { Spacing } from "../../theme/spacing";
import { Container } from "../../components/base/Container";

const FAB_SIZE = 56;

const PAGE_SIZE = 20;

const THROTTLE_MS = 300;

export default function List() {
	const router = useRouter();
	const { width: screenWidth } = useWindowDimensions();
	const gap = Spacing.sm;
	const itemWidth = 300;
	const availableWidth = screenWidth - Spacing.md * 2;
	const numColumns = Math.max(1, Math.floor((availableWidth + gap) / (itemWidth + gap)));
	const colW = (availableWidth - (numColumns - 1) * gap) / numColumns;

	const {
		events,
		fetchNextPage,
		hasNextPage,
		isFetchingNextPage,
		isLoading,
	} = useEventListInfiniteQuery({
		pageSize: PAGE_SIZE,
		orderBy: "name",
	});

	const lastScroll = useRef(0);

	const handleScroll = useCallback(
		(e: NativeSyntheticEvent<NativeScrollEvent>) => {
			const now = Date.now();
			if (now - lastScroll.current < THROTTLE_MS) return;
			lastScroll.current = now;

			const { layoutMeasurement, contentOffset, contentSize } = e.nativeEvent;
			if (layoutMeasurement.height + contentOffset.y >= contentSize.height - 300) {
				if (hasNextPage && !isFetchingNextPage) {
					fetchNextPage();
				}
			}
		},
		[hasNextPage, isFetchingNextPage, fetchNextPage],
	);

	if (isLoading) {
		return <EmptyState message="Loading events…" />;
	}

	return (
		<Box flex={1}>
			<Box component={ScrollView} flex={1} onScroll={handleScroll} scrollEventThrottle={THROTTLE_MS}>
				<Box pt="md" pb={FAB_SIZE + Spacing.md} gap={Spacing.sm}>
					<Container size="lg" pb={4}>
						<Text fz={24} fw="bold">
							Events
						</Text>
						<Text fz={13} c={Colors.TextDimmed}>
							{events.length} event{events.length !== 1 ? "s" : ""}
						</Text>
					</Container>

					<Box px="md">
						{(() => {
							const cols: (typeof events)[] = Array.from({ length: numColumns }, () => []);
							events.forEach((item, i) => cols[i % numColumns].push(item));
							return (
								<Box direction="row" gap={gap}>
									{cols.map((col, ci) => (
										<Box key={ci} w={colW} gap={gap}>
											{col.map((item, ii) => {
												const eventId = item.data?.id;
												return (
													<ResolvedEventContext.Provider key={`${ci}-${ii}`} value={item.data ?? null}>
														<Box>
															<EventCard
																onPress={eventId ? () => router.push(`/event/${eventId}`) : undefined}
															/>
														</Box>
													</ResolvedEventContext.Provider>
												);
											})}
										</Box>
									))}
								</Box>
							);
						})()}

						{isFetchingNextPage && (
							<Box p="md" align="center">
								<Loader size="small" />
							</Box>
						)}
					</Box>
				</Box>
			</Box>

			<Fab onPress={() => router.push("/new")} />
		</Box>
	);
}
