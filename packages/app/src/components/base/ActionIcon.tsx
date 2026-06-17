import type { ReactNode } from "react";
import { TouchableOpacity } from "react-native";
import { Box, type BoxProps } from "./Box";
import { Colors } from "../../theme/colors";
import { Radius } from "../../theme/sizing";

export type ActionIconVariant = "light" | "subtle";

export interface ActionIconProps extends BoxProps {
	children: ReactNode;
	color?: string;
	disabled?: boolean;
	onPress?: () => void;
	variant?: ActionIconVariant;
	size?: keyof typeof SIZES;
}

const SIZES = {
	xs: { p: 0, radius: Radius.sm },
	sm: { p: 6, radius: Radius.sm },
	md: { p: 10, radius: Radius.sm },
	lg: { p: 14, radius: Radius.md },
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
	const s = SIZES[size];

	return (
		<Box
			component={onPress ? TouchableOpacity : undefined}
			p={s.p}
			radius={s.radius}
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
