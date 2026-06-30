import type { ReactNode } from "react";
import { TouchableOpacity } from "react-native";
import { Box, type BoxProps } from "../Box";
import { Colors } from "../../../theme/colors";
import { ControlHeight, IconSize, Radius } from "../../../theme/sizing";

export type ActionIconVariant = "light" | "subtle";

export interface ActionIconProps extends BoxProps {
	children: ReactNode;
	color?: string;
	disabled?: boolean;
	onPress?: () => void;
	variant?: ActionIconVariant;
	size?: "auto" | keyof typeof SIZES;
}

const SIZES = {
	xs: { s: undefined, radius: Radius.xs },
	sm: { s: ControlHeight.sm, radius: Radius.sm },
	md: { s: ControlHeight.md, radius: Radius.sm },
	lg: { s: ControlHeight.lg, radius: Radius.md },
} as const;

export const ActionIcon = ({
	children,
	color = Colors.Primary,
	disabled,
	onPress,
	variant = "light",
	size = "md",
	...rest
}: ActionIconProps) => {
	const s = size === "auto" ? undefined : SIZES[size];

	return (
		<Box
			component={onPress ? TouchableOpacity : undefined}
			{...(s?.s !== undefined ? { w: s.s, h: s.s } : {})}
			radius={s?.radius}
			bg={variant === "light" ? Colors.BackgroundLight : undefined}
			align="center"
			justify="center"
			op={disabled ? 0.4 : undefined}
			activeOpacity={0.7}
			disabled={disabled}
			onPress={onPress}
			{...rest as any}
		>
			{children}
		</Box>
	);
};
