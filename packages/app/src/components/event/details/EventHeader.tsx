import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Colors } from "../../../theme/colors";
import { TransText } from "../../core/TransText";

export const EventHeader = () => {
	const { data } = useResolvedEvent();

	return (
		<Box gap={4}>
			<TransText fz={28} fw="bold" value={data?.name} />
			{data?.label && (
				<TransText fz={16} c={Colors.TextDimmed} value={data.label} />
			)}
		</Box>
	);
};
