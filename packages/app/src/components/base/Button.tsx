import { TouchableOpacity, ActivityIndicator } from "react-native";
import { Text } from "./Text";
import { Box, type BoxProps } from "./Box";
import { Colors } from "../../theme/colors";
import { FontSize, Radius } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";

interface VariantStyle {
	bg: string;
	text: string;
}

const VARIANT_STYLES = {
	light: { bg: Colors.BackgroundLight, text: Colors.Text },
	subtle: { bg: "transparent", text: Colors.Primary },
	filled: { bg: Colors.Primary, text: "#fff" },
} satisfies Record<string, VariantStyle>;

const SIZE_STYLES = {
	sm: { pv: Spacing.sm, ph: Spacing.xs, fz: FontSize.sm },

	// Match TextInput's size
	md: { pv: Spacing.sm, ph: Spacing.sm, fz: FontSize.sm },
} satisfies Record<string, { pv: number; ph: number; fz: number }>;

export interface ButtonProps extends BoxProps {
	children: React.ReactNode;
	variant?: keyof typeof VARIANT_STYLES;
	color?: string;
	size?: keyof typeof SIZE_STYLES;
	loading?: boolean;
	leftSection?: React.ReactNode;
	rightSection?: React.ReactNode;
	onPress?: () => void;
	disabled?: boolean;
}

export const Button = ({
	children,
	variant = "light",
	color,
	size = "md",
	loading = false,
	leftSection,
	rightSection,
	onPress,
	disabled,
	style,
	...rest
}: ButtonProps) => {
	const dimmed = disabled || loading;

	const vs = VARIANT_STYLES[variant];
	const ss = SIZE_STYLES[size];

	const bg = dimmed ? Colors.BackgroundLight : color && variant === "filled" ? color : vs.bg;
	const textColor = dimmed ? Colors.TextDimmed : color && variant !== "filled" ? color : vs.text;

	return (
		<Box<typeof TouchableOpacity>
			component={TouchableOpacity}
			activeOpacity={0.7}
			disabled={dimmed}
			onPress={onPress}
			direction="row"
			align="center"
			justify="center"
			gap={Spacing.xs}
			radius={Radius.Default}
			px={ss.ph}
			py={ss.pv}
			bg={bg}
			op={dimmed ? 0.4 : undefined}
			style={style}
			{...(rest as any)}
		>
			{leftSection}
			{loading && <ActivityIndicator size="small" color={textColor} />}
			{typeof children === "string" ? (
				<Text fz={ss.fz} c={textColor}>
					{children}
				</Text>
			) : (
				<Box flex={1}>{children}</Box>
			)}
			{rightSection}
		</Box>
	);
};
