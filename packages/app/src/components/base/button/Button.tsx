import { TouchableOpacity, ActivityIndicator } from "react-native";
import { Text } from "../Text";
import { Box, type BoxProps } from "../Box";
import { Colors } from "../../../theme/colors";
import { ControlHeight, FontSize, Radius } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";

export const ButtonTheme = {
	default: { bg: Colors.BackgroundLight, text: Colors.Text },
	primary: { bg: Colors.Primary, text: Colors.White },
	subtle: { bg: "transparent", text: Colors.Text },
	danger: { bg: Colors.Danger, text: Colors.White },
} as const;

export type ButtonVariant = keyof typeof ButtonTheme;

const SIZE_STYLES = {
	sm: { h: ControlHeight.sm, ph: Spacing.sm, fz: FontSize.xs },
	md: { h: ControlHeight.md, ph: Spacing.sm, fz: FontSize.sm },
	lg: { h: ControlHeight.lg, ph: Spacing.md, fz: FontSize.md },
} satisfies Record<string, { h: number; ph: number; fz: number }>;

export interface ButtonProps extends BoxProps {
	children: React.ReactNode;
	variant?: ButtonVariant;
	selected?: boolean;
	color?: string;
	size?: keyof typeof SIZE_STYLES;
	loading?: boolean;
	leftSection?: React.ReactNode;
	rightSection?: React.ReactNode;
	onPress?: () => void;
	disabled?: boolean;
}

const stringLikeChildren = (children: React.ReactNode): children is string | number => {
	if (Array.isArray(children)) return children.every(stringLikeChildren);
	return typeof children === "string" || typeof children === "number";
};

export const Button = ({
	children,
	variant = "default",
	selected,
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

	const vs = ButtonTheme[variant];
	const ss = SIZE_STYLES[size];

	const useSelected = selected && (variant === "default" || variant === "subtle");

	const bg = useSelected
		? (color ?? Colors.PrimaryTint)
		: (color && variant === "primary")
			? color
			: vs.bg;

	const textColor = useSelected
		? Colors.Text
		: (color && variant !== "primary")
			? color
			: vs.text;

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
			bg={bg}
			op={dimmed ? 0.4 : undefined}
			mih={ss.h}
			style={style}
			{...(rest as any)}
		>
			{leftSection}
			{loading && <ActivityIndicator size="small" color={textColor} />}
			{stringLikeChildren(children) ? (
				<Text fz={ss.fz} c={textColor}>
					{children}
				</Text>
			) : (
				<Box flex={1} direction="row" align="center" gap={rest.gap}>
					{children}
				</Box>
			)}
			{rightSection}
		</Box>
	);
};
