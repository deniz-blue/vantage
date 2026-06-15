import { type ViewStyle } from "react-native";
import { IconPlus } from "@tabler/icons-react-native";
import { ActionIcon, type ActionIconProps } from "./ActionIcon";
import { resolveColor } from "../../theme/colors";
import { Sizing } from "../../theme/sizing";

export interface FabProps extends Omit<ActionIconProps, "children"> {
	icon?: React.ReactNode;
	color?: string;
	size?: number;
}

const shadow: ViewStyle = {
	elevation: 6,
	shadowColor: "#000",
	shadowOffset: { width: 0, height: 3 },
	shadowOpacity: 0.3,
	shadowRadius: 4,
};

export const Fab = ({ icon, color = "Primary", size = 56, style, ...rest }: FabProps) => (
	<ActionIcon
		pos="absolute"
		right={16}
		bottom={16}
		w={size}
		h={size}
		radius={Sizing.radiusXl}
		bg={resolveColor(color)}
		style={[shadow, style]}
		{...(rest as any)}
	>
		{icon ?? <IconPlus width={size * 0.5} height={size * 0.5} color="#fff" />}
	</ActionIcon>
);
