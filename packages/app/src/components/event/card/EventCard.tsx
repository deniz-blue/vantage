import { Card } from "../../base/Card";
import { Box } from "../../base/Box";
import { EventCardTitle } from "./EventCardTitle";
import { EventCardSummary } from "./EventCardSummary";
import { EventBackground } from "../EventBackground";
import { ButtonBase } from "../../base/ButtonBase";
import { memo } from "react";

export interface EventCardProps {
	onPress?: () => void;
	onLongPress?: () => void;
}

export const EventCard = memo(({ onPress, onLongPress }: EventCardProps) => {
	return (
		<ButtonBase onPress={onPress} onLongPress={onLongPress} style={{ flex: 1 }}>
			<Card flex={1} bg="Dark6" style={{ overflow: "hidden" }}>
				<EventBackground />
				<Box direction="row" align="center" gap={6}>
					<EventCardTitle />
				</Box>
				<EventCardSummary />
			</Card>
		</ButtonBase>
	);
});
