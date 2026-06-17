import { TouchableOpacity } from "react-native";
import { Card } from "../../base/Card";
import { Box } from "../../base/Box";
import { EventCardTitle } from "./EventCardTitle";
import { EventCardSummary } from "./EventCardSummary";

export interface EventCardProps {
	onPress?: () => void;
}

export const EventCard = ({ onPress }: EventCardProps) => (
	<Box component={TouchableOpacity} onPress={onPress} disabled={!onPress} activeOpacity={0.7} flex={1}>
		<Card flex={1} bg="Dark6">
			<Box direction="row" align="center" gap={6}>
				<EventCardTitle />
			</Box>
			<EventCardSummary />
		</Card>
	</Box>
);
