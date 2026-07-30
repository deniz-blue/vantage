import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { FontSize } from "../../../theme/sizing";

export const EventCardBadges = () => {
	const { error } = useResolvedEvent();

	return (
		<Box direction="row" gap={4}>
			{error && (
				<Text fz={FontSize.xs} c="Error" fw="bold">
					E
				</Text>
			)}
		</Box>
	);
};
