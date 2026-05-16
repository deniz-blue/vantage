import { Box, Indicator, Paper, Stack, Text } from "@mantine/core";
import { useState } from "react";
import { useEventListQuery, useEventQueries } from "@vantage/core";
import { EventCard, type EventCardProps } from "../../components/content/event/card/EventCard";
import { createFileRoute } from "@tanstack/react-router";
import { CalendarMonth } from "../../components/calendar/CalendarMonth";
import { CalendarMobileMonth } from "../../components/calendar/CalendarMobileMonth";
import { ResolvedEventContext } from "@vantage/core";

export const Route = createFileRoute("/_layout/calendar")({
	component: CalendarPage,
	staticData: {
		spaceless: true,
	},
})

export default function CalendarPage() {
	const h = "calc(100svh - var(--app-shell-header-height, 0px) - 2 * var(--app-shell-padding) - var(--safe-area-inset-top) - var(--safe-area-inset-bottom))";

	const [month, setMonth] = useState<`${number}-${number}`>(Temporal.Now.plainDateISO().toString().slice(0, 7) as `${number}-${number}`);
	const [day, setDay] = useState<`${number}-${number}-${number}`>(Temporal.Now.plainDateISO().toString() as `${number}-${number}-${number}`);

	let breakpoint = "xs";

	return (
		<Stack
			h={h}
			mah={h}
			align="center"
			justify="center"
		>
			<Box visibleFrom={breakpoint} w="100%" h="100%">
				<CalendarMonth
					month={month}
					setMonth={(m) => setMonth(m)}
					renderDay={({ day }) => <DayCard day={day} variant="inline" />}
				/>
			</Box>
			<Box hiddenFrom={breakpoint} w="100%" h="100%">
				<CalendarMobileMonth
					month={month}
					setMonth={(m) => setMonth(m)}
					day={day}
					setDay={(d) => setDay(d)}
					renderDay={({ day }) => <DayCard
						day={day}
						variant="card"
					/>}
					renderDayButton={DayButton}
				/>
			</Box>
		</Stack>
	)
};

export const DayButton = ({
	day,
}: {
	day: `${number}-${number}-${number}`;
}) => {
	const ids = useEventListQuery({
		beforeTimestamp: Temporal.PlainDate.from(day).add({ days: 1 }).toZonedDateTime({ timeZone: "UTC" }).toInstant().epochMilliseconds,
		afterTimestamp: Temporal.PlainDate.from(day).toZonedDateTime({ timeZone: "UTC" }).toInstant().epochMilliseconds,
	});

	return (
		<Indicator
			label={ids.data?.length ?? 0}
			size={16}
			position="bottom-center"
			offset={{ x: 0, y: -4 }}
			disabled={ids.data?.length === 0}
			showZero={false}
			color="var(--mantine-color-blue-light)"
		>
			<Paper
				component="div"
				bg="transparent"
			>
				<Text>{day.slice(8)}</Text>
			</Paper>
		</Indicator>
	);
};

export const DayCard = ({
	day,
	variant,
}: {
	day: `${number}-${number}-${number}`;
	variant?: EventCardProps["variant"];
}) => {
	const lowTimestamp = Temporal.PlainDate.from(day).toZonedDateTime({ timeZone: "UTC" }).toInstant().epochMilliseconds;
	const highTimestamp = Temporal.PlainDate.from(day).add({ days: 1 }).toZonedDateTime({ timeZone: "UTC" }).toInstant().epochMilliseconds - 1;
	const ids = useEventListQuery({
		beforeTimestamp: highTimestamp,
		afterTimestamp: lowTimestamp,
	});

	const queries = useEventQueries(ids.data ?? []);

	return (
		<Stack gap={0}>
			{queries.map((query, index) => (
				<ResolvedEventContext key={index} value={query.data ?? null}
				>
					<EventCard
						variant={variant}
						loading={query.isFetching}
					/>
				</ResolvedEventContext>
			))}
		</Stack>
	);
};
