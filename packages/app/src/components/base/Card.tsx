import type { ReactNode } from "react";
import { Box, type BoxProps } from "./Box";
import { Colors } from "../../theme/colors";
import { Radius } from "../../theme/sizing";

export interface CardProps extends BoxProps {
	children: ReactNode;
}

export const Card = ({
	children,
	p = "sm",
	radius = Radius.Default,
	style,
	...rest
}: CardProps) => (
	<Box
		radius={radius}
		p={p}
		style={[
			{
				borderWidth: 1,
				borderColor: Colors.Dark4,
			},
			style,
		]}
		{...rest}
	>
		{children}
	</Box>
);
