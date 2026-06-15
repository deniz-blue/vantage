import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { Sizing } from "../../../theme/sizing";

export const EventCardTitle = () => {
	const { data } = useResolvedEvent();

	return (
		<Box flex={1}>
			<TransText
				fw="bold"
				fz={Sizing.fontSizeMd}
				numberOfLines={1}
				value={data?.name}
				fallback={<Text fz={Sizing.fontSizeMd} fst="italic" c="TextDimmed">Untitled event</Text>}
			/>
			{data?.label && (
				<TransText
					fz={Sizing.fontSizeMd}
					c="TextDimmed"
					numberOfLines={1}
					value={data.label}
				/>
			)}
		</Box>
	);
};
