import { Anchor, Box, Center, Divider, Group, ScrollArea, Stack, Text, Title } from "@mantine/core";
import { useEventListQuery, useEventQueries } from "@vantage/core";
import { EventCard } from "../../../content/event/card/EventCard";
import { EventContextMenu } from "../../../content/event/EventContextMenu";
import { ResolvedEventContext } from "@vantage/core";
import { Link } from "@tanstack/react-router";

export const WidgetUpcomingEvents = () => {
	const currentTimeRoundedMinute = Math.floor(Date.now() / (60 * 1000)) * (60 * 1000);

	const ids = useEventListQuery({
		afterTimestamp: currentTimeRoundedMinute,
		limit: 10,
		orderBy: "instanceStart",
	});

	const queries = useEventQueries(ids.data || []);

	return (
		<Stack gap={4}>
			<Group gap={4}>
				<Title order={4} c="dimmed">
					Upcoming Events
				</Title>
				<Divider
					labelPosition="left"
					variant="dashed"
					flex="1"
				/>
				<Anchor
					inline
					inherit
					renderRoot={(props) => (
						<Link
							to="/list"
							{...props}
						/>
					)}
				>
					View All
				</Anchor>
			</Group>
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
