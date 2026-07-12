import type { ReactNode } from "react";
import { Colors } from "../../theme/colors";
import { Box, type BoxProps } from "./Box";

export interface DividerProps extends BoxProps {
	color?: string;
	thickness?: number;
	/** @default false — horizontal line */
	vertical?: boolean;
	/** Label in the center of the divider line. */
	label?: ReactNode;
	/** Label on the left side of the divider line. */
	leftSection?: ReactNode;
	/** Label on the right side of the divider line. */
	rightSection?: ReactNode;
}

const Line = ({
	color,
	thickness,
	...rest
}: {
	color: string;
	thickness: number;
} & BoxProps) => <Box h={thickness} flex={1} bg={color} {...rest} />;

export const Divider = ({
	color = Colors.BackgroundLight,
	thickness = 1,
	vertical = false,
	my,
	mx,
	label,
	leftSection,
	rightSection,
	...rest
}: DividerProps) => {
	if (vertical) {
		return <Box w={thickness} h="100%" mx={mx ?? "sm"} bg={color} {...rest} />;
	}

	if (!label && !leftSection && !rightSection) {
		return <Box h={thickness} w="100%" my={my ?? "sm"} bg={color} {...rest} />;
	}

	return (
		<Box direction="row" align="center" my={my ?? "sm"} {...rest}>
			{leftSection}
			<Line
				color={color}
				thickness={thickness}
				ml={leftSection ? "sm" : undefined}
				mr={label ? "sm" : undefined}
			/>
			{label}
			<Line
				color={color}
				thickness={thickness}
				ml={label ? "sm" : undefined}
				mr={rightSection ? "sm" : undefined}
			/>
			{rightSection}
		</Box>
	);
};
