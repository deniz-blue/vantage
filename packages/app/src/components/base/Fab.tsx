import { type ViewStyle } from "react-native";
import { ActionIcon, type ActionIconProps } from "./button/ActionIcon";
import { resolveColor } from "../../theme/colors";
import { Radius } from "../../theme/sizing";
import { Box, type BoxProps } from "./Box";

export interface FabProps extends Omit<ActionIconProps, "children"> {
	wrapperProps?: BoxProps;
	icon?: React.ReactNode;
	color?: string;
}

const shadow: ViewStyle = {
	elevation: 6,
	shadowColor: "#000",
	shadowOffset: { width: 0, height: 3 },
	shadowOpacity: 0.3,
	shadowRadius: 4,
};

const FAB_SIZE = 56;

export const Fab = ({
	icon,
	color = "Primary",
	style,
	wrapperProps: { style: wrapperStyle, ...wrapperProps } = {},
	...rest
}: FabProps) => {
	return (
		<Box
			absoluteFill
			align="flex-end"
			justify="flex-end"
			p="md"
			style={[{ zIndex: 100 }, wrapperStyle]}
			{...wrapperProps}
		>
			<ActionIcon
				w={FAB_SIZE}
				h={FAB_SIZE}
				radius={Radius.xl}
				bg={resolveColor(color)}
				style={[shadow, style]}
				{...(rest as any)}
			>
				{icon}
			</ActionIcon>
		</Box>
	);
};
