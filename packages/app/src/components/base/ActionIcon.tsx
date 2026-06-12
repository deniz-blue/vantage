import type { ReactNode } from "react";
import { TouchableOpacity, type TouchableOpacityProps, type ViewStyle } from "react-native";
import { Colors } from "../../theme/colors";
import { Sizing } from "../../theme/sizing";

export interface ActionIconProps extends Omit<TouchableOpacityProps, "children"> {
	children: ReactNode;
	color?: string;
	disabled?: boolean;
}

export const ActionIcon = ({
	children,
	color = Colors.Primary,
	disabled,
	style,
	...rest
}: ActionIconProps) => {
	return (
		<TouchableOpacity
			activeOpacity={0.7}
			disabled={disabled}
			style={[
				{
					width: Sizing.md,
					height: Sizing.md,
					borderRadius: Sizing.radiusSm,
					alignItems: "center",
					justifyContent: "center",
					backgroundColor: Colors.BackgroundLight,
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
