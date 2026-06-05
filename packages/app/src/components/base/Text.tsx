import { TextProps as RNTextProps, Text as RNText } from "react-native";
import { Colors, getThemeColor, ThemeColor } from "../../theme/colors";

export interface TextProps extends RNTextProps {
	c?: ThemeColor;
};

export const Text = (props: TextProps) => {
	const style: TextProps["style"] = {
		fontFamily: "LexendDeca-Regular",
	};

	if (props.c) style.color = getThemeColor(props.c);
	else style.color = Colors.Text;

	return (
		<RNText
			style={[style, props.style]}
			{...props}
		/>
	);
};
