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
	fill?: boolean;
}

export const EventCard = memo(({ onPress, onLongPress, fill }: EventCardProps) => {
	return (
		<ButtonBase onPress={onPress} onLongPress={onLongPress} style={fill ? { flex: 1 } : undefined}>
			<Card bg="Dark6" style={{ overflow: "hidden", flex: fill ? 1 : undefined }}>
				<EventBackground />
				<Box direction="row" align="center" gap={6}>
					<EventCardTitle />
				</Box>
				<EventCardSummary />
			</Card>
		</ButtonBase>
	);
});
