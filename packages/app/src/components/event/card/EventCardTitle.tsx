import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";

export const EventCardTitle = () => {
	const { data } = useResolvedEvent();

	return (
		<Box flex={1}>
			<TransText
				fw="bold"
				fz={16}
				numberOfLines={1}
				value={data?.name}
				fallback={<Text fz={14} style={{ fontStyle: "italic" }} c="TextDimmed">Untitled event</Text>}
			/>
			{data?.label && (
				<TransText
					fz={13}
					c="TextDimmed"
					numberOfLines={1}
					value={data.label}
				/>
			)}
		</Box>
	);
};
