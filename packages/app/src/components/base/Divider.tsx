import { Colors } from "../../theme/colors";
import { Box, type BoxProps } from "./Box";

export interface DividerProps extends BoxProps {
	color?: string;
	thickness?: number;
	vertical?: boolean;
}

export const Line = ({
	color = Colors.BackgroundLight,
	thickness = 2,
	vertical = false,
	...rest
}: {
	color?: string;
	thickness?: number;
	vertical?: boolean;
} & BoxProps) => (
	<Box
		w={vertical ? thickness : undefined}
		h={vertical ? undefined : thickness}
		flex={1}
		bg={color}
		{...rest}
	/>
);

export const Divider = ({ color, thickness, vertical, ...rest }: DividerProps) => {
	return (
		<Box direction={vertical ? "column" : "row"} align="center" my="sm" {...rest}>
			<Line color={color} thickness={thickness} vertical={vertical} />
		</Box>
	);
};
