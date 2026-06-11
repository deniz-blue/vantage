import type { ReactNode } from "react";
import type { ViewStyle } from "react-native";
import { Box, type Spacing } from "./Box";
import { Colors } from "../../theme/colors";

export interface CardProps {
	children: ReactNode;
	p?: Spacing;
	radius?: number;
	bg?: string;
	style?: ViewStyle;
}

export const Card = ({
	children,
	p = "md",
	radius = 8,
	bg = Colors.BackgroundLight,
	style,
}: CardProps) => (
	<Box bg={bg} radius={radius} p={p} style={style}>
		{children}
	</Box>
);
