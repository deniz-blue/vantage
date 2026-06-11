import {
	TouchableOpacity,
	type TouchableOpacityProps,
	ActivityIndicator,
	ViewStyle,
} from "react-native";
import { Text } from "./Text";
import { Colors } from "../../theme/colors";

export interface ButtonProps extends Omit<TouchableOpacityProps, "children"> {
	children: string;
	variant?: "filled" | "light" | "subtle" | "outline";
	color?: string;
	size?: "sm" | "md" | "lg";
	loading?: boolean;
	fullWidth?: boolean;
}

export const Button = ({
	children,
	variant = "filled",
	color = Colors.Primary,
	size = "md",
	loading = false,
	fullWidth = false,
	disabled,
	style,
	...rest
}: ButtonProps) => {
	const isDisabled = disabled || loading;

	const bgColor = variant === "filled"
		? color
		: "transparent";

	const borderColor = variant === "outline"
		? color
		: "transparent";

	const textColor = variant === "filled"
		? "#fff"
		: color;

	const paddings: Record<string, ViewStyle> = {
		sm: { paddingVertical: 6, paddingHorizontal: 12 },
		md: { paddingVertical: 10, paddingHorizontal: 16 },
		lg: { paddingVertical: 14, paddingHorizontal: 20 },
	};

	const fontSize = { sm: 13, md: 15, lg: 17 };

	return (
		<TouchableOpacity
			activeOpacity={0.7}
			disabled={isDisabled}
			style={[
				{
					backgroundColor: isDisabled ? Colors.BackgroundLight : bgColor,
					borderRadius: 8,
					borderWidth: variant === "outline" ? 1.5 : 0,
					borderColor: isDisabled ? "transparent" : borderColor,
					alignItems: "center",
					justifyContent: "center",
					flexDirection: "row",
					gap: 8,
					opacity: isDisabled ? 0.4 : 1,
				},
				paddings[size],
				fullWidth ? { width: "100%" } : undefined,
				style as ViewStyle,
			]}
			{...rest}
		>
			{loading && (
				<ActivityIndicator size="small" color={textColor} />
			)}
			<Text
				style={{
					fontSize: fontSize[size],
					fontWeight: "600",
					color: isDisabled ? Colors.TextDimmed : textColor,
				}}
			>
				{children}
			</Text>
		</TouchableOpacity>
	);
};
