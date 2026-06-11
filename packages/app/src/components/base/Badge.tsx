import type { ReactNode } from "react";
import type { ViewStyle } from "react-native";
import { View } from "react-native";
import { Text } from "./Text";
import { Colors, resolveColor } from "../../theme/colors";

export interface BadgeProps {
	children: string;
	variant?: "light" | "filled" | "outline" | "dot";
	color?: string;
	size?: "sm" | "md" | "lg";
	leftSection?: ReactNode;
	rightSection?: ReactNode;
	fullWidth?: boolean;
	style?: ViewStyle;
}

export const Badge = ({
	children,
	variant = "light",
	color = Colors.Primary,
	size = "md",
	leftSection,
	rightSection,
	fullWidth,
	style,
}: BadgeProps) => {
	const c = resolveColor(color);
	const { bg, border, text } = variantStyles(variant, c);

	const sizeStyles = sizeMap[size];

	return (
		<View
			style={[
				{
					backgroundColor: bg,
					borderRadius: 16,
					borderWidth: variant === "outline" ? 1 : 0,
					borderColor: border,
					alignItems: "center",
					justifyContent: "center",
					flexDirection: "row",
					gap: 4,
					alignSelf: fullWidth ? "stretch" : "flex-start",
				},
				sizeStyles.container,
				style,
			]}
		>
			{variant === "dot" && (
				<View
					style={{
						width: 8,
						height: 8,
						borderRadius: 4,
						backgroundColor: c,
					}}
				/>
			)}
			{leftSection}
			<Text
				style={{
					fontSize: sizeStyles.fontSize,
					fontWeight: "600",
					color: text,
				}}
			>
				{children}
			</Text>
			{rightSection}
		</View>
	);
};

// === Variant styles ===

const variantStyles = (variant: BadgeProps["variant"], color: string) => {
	switch (variant) {
		case "filled":
			return { bg: color, border: "transparent", text: "#fff" };
		case "outline":
			return { bg: "transparent", border: color, text: color };
		case "dot":
			return { bg: color + "18", border: "transparent", text: color };
		default:
			return { bg: color + "22", border: "transparent", text: color };
	}
};

// === Size map ===

const sizeMap = {
	sm: {
		container: { paddingHorizontal: 8, paddingVertical: 2 } as ViewStyle,
		fontSize: 11,
	},
	md: {
		container: { paddingHorizontal: 10, paddingVertical: 4 } as ViewStyle,
		fontSize: 12,
	},
	lg: {
		container: { paddingHorizontal: 14, paddingVertical: 6 } as ViewStyle,
		fontSize: 14,
	},
};
