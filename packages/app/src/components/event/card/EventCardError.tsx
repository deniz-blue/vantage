import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Sizing, FontSize } from "../../../theme/sizing";

export const EventCardError = () => {
	const { error } = useResolvedEvent();

	if (!error) return null;

	return (
		<Box direction="row" gap={4} mt={4}>
			<Text fz={FontSize.md} c="Red">
				{error.kind ?? "error"}: {error.message}
			</Text>
		</Box>
	);
};
