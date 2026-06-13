import { Box } from "../../base/Box";
import { EventError } from "./EventError";
import { EventHeader } from "./EventHeader";

export const EventDetails = () => (
	<Box flex={1}>
		<EventError />
		<EventHeader />
	</Box>
);
