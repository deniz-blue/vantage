import type { ReactNode } from "react";
import { TouchableOpacity } from "react-native";
import { Box, type BoxProps } from "./Box";
import { Colors } from "../../theme/colors";
import { Sizing } from "../../theme/sizing";

export interface ActionIconProps extends BoxProps {
	children: ReactNode;
	color?: string;
	disabled?: boolean;
	onPress?: () => void;
}

export const ActionIcon = (props: ActionIconProps) => {
	const { children, color = Colors.Primary, disabled, onPress, ...boxProps } = props;

	return (
		<Box
			component={TouchableOpacity}
			w={Sizing.md}
			h={Sizing.md}
			radius={Sizing.radiusSm}
			bg={Colors.BackgroundLight}
			align="center"
			justify="center"
			op={disabled ? 0.4 : undefined}
			activeOpacity={0.7}
			disabled={disabled}
			onPress={onPress}
			{...boxProps as any}
		>
			{children}
		</Box>
	);
};
