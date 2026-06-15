import type { ReactNode } from "react";
import { TouchableOpacity } from "react-native";
import { Box, type BoxProps } from "./Box";
import { Colors } from "../../theme/colors";
import { Sizing } from "../../theme/sizing";

export type ActionIconVariant = "light" | "subtle";

export interface ActionIconProps extends BoxProps {
	children: ReactNode;
	color?: string;
	disabled?: boolean;
	onPress?: () => void;
	variant?: ActionIconVariant;
}

export const ActionIcon = (props: ActionIconProps) => {
	const { children, color = Colors.Primary, disabled, onPress, variant = "light", ...boxProps } = props;

	return (
		<Box
			component={TouchableOpacity}
			w={Sizing.md}
			h={Sizing.md}
			radius={Sizing.radiusSm}
			bg={variant === "light" ? Colors.BackgroundLight : undefined}
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
