import type { ReactNode } from "react";
import { TouchableOpacity, type TouchableOpacityProps, type ViewStyle } from "react-native";
import { Colors } from "../../theme/colors";
import { resolveColor } from "../../theme/colors";

export interface ActionIconProps extends Omit<TouchableOpacityProps, "children"> {
	children: ReactNode;
	color?: string;
	variant?: "subtle" | "filled" | "light" | "outline";
	size?: "sm" | "md" | "lg";
	disabled?: boolean;
}

export const ActionIcon = ({
	children,
	color = Colors.Primary,
	variant = "subtle",
	size = "md",
	disabled,
	style,
	...rest
}: ActionIconProps) => {
	const resolvedColor = resolveColor(color);

	const variantStyle = variantStyles(variant, resolvedColor);
	const sizeVal = sizeMap[size];

	return (
		<TouchableOpacity
			activeOpacity={0.7}
			disabled={disabled}
			style={[
				{
					width: sizeVal,
					height: sizeVal,
					borderRadius: sizeVal / 2,
					alignItems: "center",
					justifyContent: "center",
					backgroundColor: variantStyle.bg,
					borderWidth: variant === "outline" ? 1 : 0,
					borderColor: variantStyle.border,
					opacity: disabled ? 0.4 : 1,
				} as ViewStyle,
				style as ViewStyle,
			]}
			{...rest}
		>
			{children}
		</TouchableOpacity>
	);
};

const variantStyles = (variant: string, color: string) => {
	switch (variant) {
		case "filled": return { bg: color, border: "transparent" };
		case "light": return { bg: color + "22", border: "transparent" };
		case "outline": return { bg: "transparent", border: color };
		default: return { bg: "transparent", border: "transparent" };
	}
};

const sizeMap: Record<string, number> = {
	sm: 32,
	md: 40,
	lg: 48,
};
