import { Box, Center, Divider, Group, ScrollArea, Stack, Text, Title } from "@mantine/core";
import { useEventQueries } from "../../../../db/useEventQuery";
import { useShallow } from "zustand/react/shallow";
import { useCacheEventsStore } from "../../../../lib/cache/useCacheEventsStore";
import { EventCard } from "../../../content/event/card/EventCard";
import { EventContextMenu } from "../../../content/event/EventContextMenu";
import type { PlainDateString } from "@evnt/partial-date";
import { ResolvedEventContext } from "../../../../db/resolved-event";

export const WidgetUpcomingEvents = () => {
	const today = Temporal.Now.plainDateISO().toString() as PlainDateString;

	const firstFiveUpcomingEvents = useCacheEventsStore(
		useShallow(state => {
			const keys = Object.keys(state.cache.byWallDay) as PlainDateString[];
			const upcomingKeys = keys.filter(key => key >= today).sort().slice(0, 5);
			const events = upcomingKeys
				.map((key) => state.cache.byWallDay[key]!)
				.map(set => [...set])
				.flat();
			return [...new Set(events)];
		})
	);

	const queries = useEventQueries(firstFiveUpcomingEvents);

	return (
		<Stack gap={4}>
			<Divider
				labelPosition="left"
				variant="dashed"
				label={(
					<Title order={4}>
						Upcoming Events
					</Title>
				)}
			/>
			{queries.length === 0 && (
				<Center ta="center" w="100%">
					<Text
						c="dimmed"
						my="xs"
					>
						No upcoming events
					</Text>
				</Center>
			)}
			<ScrollArea.Autosize maw="100%" scrollbars="x" offsetScrollbars p={4}>
				<Group
					wrap="nowrap"
					// mih={300}
					align="stretch"
				>
					{queries.map((query, index) => (
						<Box
							key={index}
							miw={300}
							bg="dark"
						>
							<ResolvedEventContext value={query.data ?? null}>
								<EventCard
									variant="card"
									loading={query.isFetching}
									menu={<EventContextMenu />}
								/>
							</ResolvedEventContext>
						</Box>
					))}
				</Group>
			</ScrollArea.Autosize>
		</Stack>
	);
};
