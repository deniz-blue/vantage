import Feather from "@expo/vector-icons/Feather";
import { getThemeColor, ThemeColor } from "../../theme/colors";

export interface IconProps extends React.ComponentProps<typeof Feather> {
	c?: ThemeColor;
};

export const Icon = (props: IconProps) => {
	const DEFAULT_ICON_SIZE = 18;

	return (
		<Feather
			size={props.size || DEFAULT_ICON_SIZE}
			style={[{
				color: props.c ? getThemeColor(props.c) : undefined,
			}, props.style]}
			{...props}
		/>
	);
};
