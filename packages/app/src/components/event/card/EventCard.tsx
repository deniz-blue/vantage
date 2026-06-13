import { TouchableOpacity } from "react-native";
import { Card } from "../../base/Card";
import { Box } from "../../base/Box";
import { EventCardTitle } from "./EventCardTitle";
import { EventCardMeta } from "./EventCardMeta";
import { EventCardDate } from "./EventCardDate";
import { EventCardError } from "./EventCardError";

export interface EventCardProps {
	onPress?: () => void;
}

export const EventCard = ({ onPress }: EventCardProps) => (
	<TouchableOpacity onPress={onPress} disabled={!onPress} activeOpacity={0.7}>
		<Card>
			<Box direction="row" align="center" gap={6}>
				<EventCardTitle />
				<EventCardMeta />
			</Box>
			<EventCardDate />
			<EventCardError />
		</Card>
	</TouchableOpacity>
);
