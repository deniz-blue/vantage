import { useRouter } from "expo-router";
import { ScrollView, TouchableOpacity } from "react-native";
import { useEventListQuery, ResolvedEventContext } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { EventCard } from "../../event/card/EventCard";
import { FontSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { Divider } from "../../base/Divider";

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
			<Divider
				px="md"
				leftSection={<Text fw="bold">Upcoming Events</Text>}
				rightSection={(
					<TouchableOpacity onPress={() => router.push("/list")}>
						<Text fz={13} c="TextDimmed">View All</Text>
					</TouchableOpacity>
				)}
			/>

			{events.length === 0 ? (
				<Box py="md" align="center">
					<Text fz={FontSize.sm} c="TextDimmed">No upcoming events</Text>
				</Box>
			) : (
				<Box component={ScrollView} horizontal>
					<Box direction="row" gap="sm" px="md">
						{events.map((query, index) => (
							<Box key={index} w={280}>
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
