import { Colors } from "../../../theme/colors";
import { ControlHeight, Radius } from "../../../theme/sizing";
import { Box, BoxProps } from "../Box";

export interface InputBaseProps extends BoxProps {
	size?: keyof typeof ControlHeight;
	focused?: boolean;
};

export const InputBase = ({
	size = "md",
	focused = false,
	style,
	...rest
}: InputBaseProps) => {
	const mih = ControlHeight[size];

	return (
		<Box
			direction="row"
			align="center"
			mih={mih}
			style={[
				{
					backgroundColor: Colors.BackgroundInput,
					borderRadius: Radius.Default,
					borderWidth: 1,
					outlineWidth: focused ? 2 : 0,
					outlineStyle: "solid",
					outlineColor: Colors.Primary,
				},
				style,
			]}
			{...rest}
		/>
	);
};
