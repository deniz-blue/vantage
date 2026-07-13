import { Card } from "../../base/Card";
import { Box } from "../../base/Box";
import { EventCardTitle } from "./EventCardTitle";
import { EventCardSummary } from "./EventCardSummary";
import { EventBackground } from "../EventBackground";
import { useState } from "react";
import { EventActionsSheet } from "../../app/EventActionsSheet";
import { ButtonBase } from "../../base/ButtonBase";

export interface EventCardProps {
	onPress?: () => void;
}

export const EventCard = ({ onPress }: EventCardProps) => {
	const [open, setOpen] = useState(false);

	return (
		<ButtonBase onPress={onPress} onLongPress={() => setOpen(true)} style={{ flex: 1 }}>
			<Card flex={1} bg="Dark6" style={{ overflow: "hidden" }}>
				<EventBackground />
				<Box direction="row" align="center" gap={6}>
					<EventCardTitle />
				</Box>
				<EventCardSummary />
			</Card>

			<EventActionsSheet open={open} onClose={() => setOpen(false)} />
		</ButtonBase>
	);
};
