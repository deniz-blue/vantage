import type { PropsWithChildren } from "react";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Sizing, FontSize } from "../../../theme/sizing";

export const SmallTitle = ({ children }: PropsWithChildren) => (
	<Box py={4}>
		<Text fz={FontSize.sm} fw="bold" c="TextDimmed" tt="uppercase">
			{children}
		</Text>
	</Box>
);
