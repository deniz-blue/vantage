import { useWindowDimensions, ScrollView } from "react-native";
import { Box } from "../../base/Box";
import { EventDetailsError } from "./EventDetailsError";
import { EventDetailsBanner } from "./EventDetailsBanner";
import { EventDetailsInstanceList } from "./EventDetailsInstanceList";
import { EventDetailsActions } from "./EventDetailsActions";
import { EventDetailsLinks } from "./EventDetailsLinks";
import { EventDetailsRichtext } from "./EventDetailsRichtext";
import { EventDetailsSource } from "./EventDetailsSource";

const WIDE_BREAKPOINT = 768;

export const EventDetails = () => {
	const { width: screenWidth } = useWindowDimensions();
	const isWide = screenWidth >= WIDE_BREAKPOINT;

	const main = (
		<Box gap="md">
			<EventDetailsError />
			<EventDetailsInstanceList />
			<EventDetailsActions />
			<EventDetailsRichtext />
		</Box>
	);

	const sidebar = (
		<Box gap="md">
			<EventDetailsLinks />
			<EventDetailsSource />
		</Box>
	);

	return (
		<Box component={ScrollView} flex={1} stickyHeaderIndices={[0]}>
			<EventDetailsBanner />
			<Box gap="md" p="md">
				{isWide ? (
					<Box direction="row" gap="md">
						<Box flex={2}>{main}</Box>
						<Box flex={1}>{sidebar}</Box>
					</Box>
				) : (
					<>
						{main}
						{sidebar}
					</>
				)}
			</Box>
		</Box>
	);
};
