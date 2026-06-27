import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { FontSize } from "../../../theme/sizing";

export const EventCardTitle = () => {
	const { data } = useResolvedEvent();

	return (
		<Box flex={1}>
			<TransText
				fw="bold"
				fz={FontSize.md}
				numberOfLines={1}
				value={data?.name}
				fallback={<Text fz={FontSize.md} fst="italic" c="TextDimmed">Untitled event</Text>}
			/>
			{data?.label && (
				<TransText
					fz={FontSize.md}
					c="TextDimmed"
					numberOfLines={1}
					value={data.label}
				/>
			)}
		</Box>
	);
};
