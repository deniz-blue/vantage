import { useRouter } from "expo-router";
import { ScrollView } from "react-native";
import { useEventListQuery, ResolvedEventContext } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { EventCard } from "../../event/card/EventCard";
import { FontSize } from "../../../theme/sizing";
import { Line } from "../../base/Divider";
import { InlineTextButton } from "../../base/button/InlineTextButton";

export const WidgetUpcomingEvents = () => {
	const router = useRouter();
	const currentTimeRoundedMinute = Math.floor(Date.now() / (60 * 1000)) * (60 * 1000);

	const { events } = useEventListQuery({
		afterTimestamp: currentTimeRoundedMinute,
		limit: 10,
		orderBy: "instanceStart",
	});

	return (
		<Box gap={0}>
			<Box direction="row" align="center" px="md" py="sm" gap="sm">
				<Text fw="bold">Upcoming Events</Text>
				<Line />
				<InlineTextButton onPress={() => router.push("/list")}>View All</InlineTextButton>
			</Box>

			{events.length === 0 ? (
				<Box py="md" align="center">
					<Text fz={FontSize.sm} c="TextDimmed">
						No upcoming events
					</Text>
				</Box>
			) : (
				<Box component={ScrollView} horizontal>
					<Box direction="row" gap="sm" px="md">
						{events.map((query, index) => (
							<Box key={index} w={200}>
								<ResolvedEventContext value={query.data ?? null}>
									<EventCard
										onPress={() => {
											const id = query.data?.id;
											if (id) router.push(`/event/${id}`);
										}}
									/>
								</ResolvedEventContext>
							</Box>
						))}
					</Box>
				</Box>
			)}
		</Box>
	);
};
