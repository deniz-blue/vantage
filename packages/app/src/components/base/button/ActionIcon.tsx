import type { ReactNode } from "react";
import { Box, type BoxProps } from "../Box";
import { Colors } from "../../../theme/colors";
import { ControlHeight, Radius } from "../../../theme/sizing";
import { ButtonBase, ButtonBaseProps } from "../ButtonBase";

export type ActionIconVariant = "light" | "subtle";

export interface ActionIconProps
	extends BoxProps, Pick<ButtonBaseProps, "onPress" | "onLongPress" | "disabled"> {
	children: ReactNode;
	color?: string;
	selected?: boolean;
	disabled?: boolean | null;
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
	onPress,
	onLongPress,
	disabled,
	variant = "light",
	size = "md",
	color,
	selected,
	...rest
}: ActionIconProps) => {
	const s = size === "auto" ? undefined : SIZES[size];

	const bg = selected
		? (color ?? Colors.Primary)
		: variant === "light"
			? Colors.BackgroundLight
			: undefined;

	return (
		<ButtonBase onPress={onPress} onLongPress={onLongPress} disabled={disabled}>
			<Box
				{...(s?.s !== undefined ? { w: s.s, h: s.s } : {})}
				radius={s?.radius}
				bg={bg}
				align="center"
				justify="center"
				op={disabled ? 0.4 : undefined}
				{...rest}
			>
				{children}
			</Box>
		</ButtonBase>
	);
};
