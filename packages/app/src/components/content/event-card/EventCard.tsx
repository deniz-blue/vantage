import { useResolvedEvent } from "@vantage/core";
import { snippetEvent } from "@evnt/pretty";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Icon } from "../../base/Icon";
import { TimeDisplay } from "../time/TimeDisplay";
import { TimeRangeDisplay } from "../time/TimeRangeDisplay";
import { PartialDateDisplay } from "../time/PartialDateDisplay";
import { PartialDateRangeDisplay } from "../time/PartialDateRangeDisplay";
import { Translate } from "../Translate";

export const EventCard = () => {
	const { data } = useResolvedEvent();

	return (
		<Box
			bdr="r"
			bdc="Border"
			bg="BackgroundLight"
			p="sm"
			w="100%"
		>
			<Text>
				<Translate value={data?.name} />
			</Text>

			<EventCard.InstanceSnippets />
		</Box>
	)
};

EventCard.InstanceSnippets = () => {
	const { data } = useResolvedEvent();

	if (!data) return null;

	const snippets = snippetEvent(data);

	return (
		<Box>
			{snippets.map(({ icon, label }, i) => (
				<Box
					direction="row"
					align="center"
					gap="xs"
					key={i}
				>
					<Icon name={({
						"venue-online": "globe",
						"venue-physical": "map-pin",
						"venue-mixed": "map-pin",
						"venue-unknown": "help-circle",
						"calendar": "calendar",
						"clock": "clock",
						"": "calendar",
					} as const)[icon ?? ""]} />

					{label?.type === "text" && (
						<Text>
							{label.value}
						</Text>
					)}

					{label?.type === "time" && (
						<TimeDisplay value={label.value} />
					)}

					{label?.type === "time-range" && (
						<TimeRangeDisplay value={label.value} />
					)}

					{label?.type === "partial-date" && (
						<PartialDateDisplay value={label.value} />
					)}

					{label?.type === "date-time-range" && (
						<PartialDateRangeDisplay value={label.value} />
					)}

					{label?.type === "date-time" && (
						<PartialDateDisplay value={label.value} />
					)}
				</Box>
			))}
		</Box>
	);
};
