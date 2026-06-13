import { useResolvedEvent } from "@vantage/core";
import { Card } from "../../base/Card";
import { Text } from "../../base/Text";
import { Colors } from "../../../theme/colors";

export const EventError = () => {
	const { error } = useResolvedEvent();

	if (!error) return null;

	return (
		<Card p="sm" bg={Colors.Red + "11"} style={{ borderWidth: 1, borderColor: Colors.Red + "33" }}>
			<Text c={Colors.Red} fz={13}>
				{error.kind}: {error.message}
			</Text>
		</Card>
	);
};
