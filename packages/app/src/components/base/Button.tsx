import {
	TouchableOpacity,
	type TouchableOpacityProps,
	ActivityIndicator,
	type ViewStyle,
} from "react-native";
import { Text } from "./Text";
import { Box } from "./Box";
import { Colors } from "../../theme/colors";
import { Sizing } from "../../theme/sizing";

const VARIANT_STYLES: Record<string, { bg: string; border: string; text: string }> = {
	default: { bg: Colors.BackgroundLight, border: "transparent", text: Colors.Text },
	subtle: { bg: "transparent", border: "transparent", text: Colors.Primary },
	filled: { bg: Colors.Primary, border: "transparent", text: "#fff" },
	light: { bg: Colors.PrimaryLight + "33", border: "transparent", text: Colors.Primary },
	outline: { bg: "transparent", border: Colors.Primary, text: Colors.Primary },
};

const SIZE_STYLES: Record<string, { pv: number; ph: number; fz: number }> = {
	sm: { pv: Sizing.buttonPaddingV.sm, ph: Sizing.buttonPaddingH.sm, fz: Sizing.fontSizeSm },
	md: { pv: Sizing.buttonPaddingV.md, ph: Sizing.buttonPaddingH.md, fz: Sizing.fontSizeMd },
	lg: { pv: Sizing.buttonPaddingV.lg, ph: Sizing.buttonPaddingH.lg, fz: Sizing.fontSizeLg },
};

export interface ButtonProps extends Omit<TouchableOpacityProps, "children"> {
	children: React.ReactNode;
	variant?: keyof typeof VARIANT_STYLES;
	color?: string;
	size?: keyof typeof SIZE_STYLES;
	loading?: boolean;
	fullWidth?: boolean;
	leftSection?: React.ReactNode;
	rightSection?: React.ReactNode;
}

export const Button = ({
	children,
	variant = "filled",
	color,
	size = "md",
	loading = false,
	fullWidth = false,
	disabled,
	leftSection,
	rightSection,
	style,
	...rest
}: ButtonProps) => {
	const isDisabled = disabled || loading;

	const vs = VARIANT_STYLES[variant] ?? VARIANT_STYLES.filled!;
	const ss = SIZE_STYLES[size] ?? SIZE_STYLES.md!;

	const bgColor = color && variant === "filled" ? color : vs.bg;
	const textColor = color && variant !== "filled" ? color : vs.text;

	return (
		<TouchableOpacity
			activeOpacity={0.7}
			disabled={isDisabled}
			style={[
				{
					backgroundColor: isDisabled ? Colors.BackgroundLight : bgColor,
					borderRadius: 8,
					borderWidth: variant === "outline" ? 1.5 : 0,
					borderColor: isDisabled ? "transparent" : vs.border,
					alignItems: "center",
					justifyContent: "center",
					flexDirection: "row",
					gap: 8,
					opacity: isDisabled ? 0.4 : 1,
				},
				{ paddingVertical: ss.pv, paddingHorizontal: ss.ph },
				fullWidth ? { width: "100%" } : undefined,
				style as ViewStyle,
			]}
			{...rest}
		>
			{leftSection}
			{loading && <ActivityIndicator size="small" color={textColor} />}
			{typeof children === "string" ? (
				<Text fz={ss.fz} fw="600" c={isDisabled ? Colors.TextDimmed : textColor}>
					{children}
				</Text>
			) : (
				<Box flex={1}>{children}</Box>
			)}
			{rightSection}
		</TouchableOpacity>
	);
};
