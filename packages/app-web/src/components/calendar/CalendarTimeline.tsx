import { Button, Container, Paper, ScrollArea, Stack, Text } from "@mantine/core";
import { ResolvedEventContext, useEventListQuery, useEventQueries } from "@vantage/core";
import { useState } from "react";
import { EventCardBackground } from "../content/event/card/EventCardBackground";
import { EventCardTitle } from "../content/event/card/EventCardTitle";
import { EventStatusBadge } from "../content/event/badges/EventStatusBadge";
import { EventTimeframeBadge } from "../content/event/badges/EventTimeframeBadge";
import { EventInstanceList } from "../content/event/EventInstanceList";
import { PartialDate, PartialDateUtil } from "@evnt/partial-date";
import { EventCardBottom } from "../content/event/card/EventCardBottom";

export const CalendarTimeline = () => {
	const [lowDate, setLowDate] = useState<Temporal.PlainDate>(Temporal.Now.plainDateISO().subtract({ months: 6 }));
	const [highDate, setHighDate] = useState<Temporal.PlainDate>(Temporal.Now.plainDateISO().add({ months: 6 }));

	const lowTimestamp = lowDate.toZonedDateTime({ timeZone: "UTC" }).toInstant().epochMilliseconds;
	const highTimestamp = highDate.toZonedDateTime({ timeZone: "UTC" }).toInstant().epochMilliseconds;

	const ids = useEventListQuery({
		afterTimestamp: lowTimestamp,
		beforeTimestamp: highTimestamp,
		orderBy: "instanceStart",
	});

	const events = useEventQueries(ids.data ?? []);

	const [prevButton, nextButton] = [-1, +1].map(dir => (
		<Button
			size="lg"
			loading={ids.isFetching}
			onClick={() => {
				if (dir === -1) {
					setLowDate(d => d.subtract({ months: 6 }));
				} else {
					setHighDate(d => d.add({ months: 6 }));
				};
			}}
		>
			Load More
		</Button>
	));

	const eventsGroupedByMonth = events.reduce((groups, query) => {
		if (!query.data) return groups;

		const lowest = query.data.data?.instances?.reduce((lowest, instance) => {
			if (!instance.start || !PartialDateUtil.has(instance.start, "month")) return lowest;
			const instanceStart = PartialDateUtil.asPlainDate(instance.start as any);
			return (!lowest || instanceStart.since(lowest).sign < 0) ? instanceStart : lowest;
		}, null as Temporal.PlainDate | null) ?? null;

		if (!lowest) return groups;

		const monthKey = lowest.toLocaleString("default", { month: "long", year: "numeric" });
		if (!monthKey) return groups;

		if (!groups[monthKey]) groups[monthKey] = [];
		groups[monthKey].push(query);
		return groups;
	}, {} as Record<string, ReturnType<typeof useEventQueries>[number][]>);

	return (
		<ScrollArea.Autosize
			w="100%"
			h="100%"
			scrollbars="y"
			styles={{
				scrollbar: { zIndex: 5 },
			}}
		>
			<Stack gap={0}>
				{prevButton}

				{Object.entries(eventsGroupedByMonth).map(([month, events]) => (
					<Stack key={month} gap={0}>
						<Paper
							radius={0}
							p="xs"
							bg="dark"
							shadow="sm"
							pos="sticky"
							style={{ top: 0, zIndex: 1 }}
						>
							<Text fw="bold">{month}</Text>
						</Paper>
						<Stack gap={0}>
							{events.map((query) => (
								<ResolvedEventContext
									key={query.data?.id}
									value={query.data ?? null}
								>
									<Paper
										withBorder
										pos="relative"
										radius={0}
										py="md"
										style={{ overflow: "clip" }}
									>
										<EventCardBackground />
										<Container>
											<Stack pos="relative">
												<EventCardTitle />
												<EventStatusBadge />
												<EventTimeframeBadge />
												<EventInstanceList />
												<EventCardBottom />
											</Stack>
										</Container>
									</Paper>
								</ResolvedEventContext>
							))}
						</Stack>
					</Stack>
				))}

				{nextButton}
			</Stack>
		</ScrollArea.Autosize>
	);
};
