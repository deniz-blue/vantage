import { ElementType } from "react";
import { Colors } from "../../../theme/colors";
import { ControlHeight, Radius } from "../../../theme/sizing";
import { Box } from "../Box";
import { PolymorphicProps } from "../../../utils/polymorphic";

export type InputBaseProps<C extends ElementType> = PolymorphicProps<
	C,
	{
		size?: keyof typeof ControlHeight;
		focused?: boolean;
	}
>;

export const InputBase = <C extends ElementType>({
	component,
	size = "md",
	focused = false,
	style,
	...rest
}: InputBaseProps<C>) => {
	const mih = ControlHeight[size];

	return (
		<Box
			component={component}
			direction="row"
			align="center"
			mih={mih}
			style={[
				{
					backgroundColor: Colors.BackgroundInput,
					borderRadius: Radius.Default,
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
