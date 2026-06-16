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

const Line = ({ color, thickness }: { color: string; thickness: number }) => (
	<Box h={thickness} flex={1} bg={color} />
);

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
		<Box direction="row" align="center" gap="sm" my={my ?? "sm"} {...rest}>
			{leftSection}
			{leftSection && <Line color={color} thickness={thickness} />}
			{label && <Line color={color} thickness={thickness} />}
			{label}
			{label && <Line color={color} thickness={thickness} />}
			{rightSection && <Line color={color} thickness={thickness} />}
			{rightSection}
		</Box>
	);
};
