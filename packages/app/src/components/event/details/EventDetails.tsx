import { ScrollView } from "react-native";
import { Box } from "../../base/Box";
import { EventDetailsError } from "./EventDetailsError";
import { EventDetailsBanner } from "./EventDetailsBanner";
import { EventDetailsInstanceList } from "./EventDetailsInstanceList";
import { EventDetailsActions } from "./EventDetailsActions";
import { EventDetailsLinks } from "./EventDetailsLinks";
import { EventDetailsRichtext } from "./EventDetailsRichtext";
import { EventDetailsSource } from "./EventDetailsSource";
import { Spacing } from "../../../theme/spacing";

export const EventDetails = () => (
	<ScrollView style={{ flex: 1 }}>
		<Box px="md" pb="md" gap={Spacing.md}>
			<EventDetailsError />
			<EventDetailsBanner />
			<EventDetailsInstanceList />
			<EventDetailsActions />
			<EventDetailsRichtext />
			<EventDetailsLinks />
			<EventDetailsSource />
		</Box>
	</ScrollView>
);
