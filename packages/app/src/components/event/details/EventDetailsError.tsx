import { useResolvedEvent, VantageError } from "@vantage/core";
import { Card } from "../../base/Card";
import { Text } from "../../base/Text";
import { Colors } from "../../../theme/colors";

const bgTint = (color: string) => (color === "yellow" ? Colors.Yellow + "18" : Colors.Red + "11");
const borderTint = (color: string) =>
	color === "yellow" ? Colors.Yellow + "44" : Colors.Red + "33";
const textColor = (color: string) => (color === "yellow" ? Colors.Yellow : Colors.Red);

export const EventDetailsError = () => {
	const { error } = useResolvedEvent();

	if (!error) return null;

	const color = VantageError.getColor(error);
	const message = VantageError.getMessage(error);

	return (
		<Card p="sm" bg={bgTint(color)} style={{ borderWidth: 1, borderColor: borderTint(color) }}>
			<Text c={textColor(color)} fz={13}>
				{message}: {error.message}
			</Text>
		</Card>
	);
};
