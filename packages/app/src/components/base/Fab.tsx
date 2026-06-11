import { TouchableOpacity, type TouchableOpacityProps } from "react-native";
import { IconPlus } from "@tabler/icons-react-native";
import { resolveColor } from "../../theme/colors";

export interface FabProps extends Omit<TouchableOpacityProps, "children"> {
	icon?: React.ReactNode;
	color?: string;
	size?: number;
}

export const Fab = ({
	icon,
	color = "Primary",
	size = 56,
	style,
	...rest
}: FabProps) => (
	<TouchableOpacity
		activeOpacity={0.8}
		style={[
			{
				position: "absolute",
				right: 16,
				bottom: 16,
				width: size,
				height: size,
				borderRadius: size / 2,
				backgroundColor: resolveColor(color),
				alignItems: "center",
				justifyContent: "center",
				elevation: 6,
				shadowColor: "#000",
				shadowOffset: { width: 0, height: 3 },
				shadowOpacity: 0.3,
				shadowRadius: 4,
			},
			style,
		]}
		{...rest}
	>
		{icon ?? <IconPlus width={size * 0.5} height={size * 0.5} color="#fff" />}
	</TouchableOpacity>
);
