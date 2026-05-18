import { Stack, Text } from "@mantine/core";
import { ResolvedEventContext, useEventListQuery, useEventQueries } from "@vantage/core";
import { EventCard } from "../content/event/card/EventCard";

export const SmallEventsList = ({
	day,
}: {
	day: `${number}-${number}-${number}`;
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
						variant="inline"
						loading={query.isFetching}
					/>
				</ResolvedEventContext>
			))}
		</Stack>
	);
};
