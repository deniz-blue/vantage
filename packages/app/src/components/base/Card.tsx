import type { ReactNode } from "react";
import { Box, type BoxProps } from "./Box";
import { Colors } from "../../theme/colors";

export interface CardProps extends BoxProps {
	children: ReactNode;
}

export const Card = ({
	children,
	p = "md",
	radius = 8,
	bg = Colors.BackgroundLight,
	style,
	...rest
}: CardProps) => (
	<Box bg={bg} radius={radius} p={p} style={style} {...rest}>
		{children}
	</Box>
);
