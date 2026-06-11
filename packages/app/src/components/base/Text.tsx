import { TextProps as RNTextProps, Text as RNText } from "react-native";
import { Colors, getThemeColor, type ThemeColor } from "../../theme/colors";

export interface TextProps extends RNTextProps {
	c?: ThemeColor;
}

export const Text = (props: TextProps) => {
	const { c, style, ...rest } = props;

	return (
		<RNText
			style={[
				{
					fontFamily: "Lexend",
					color: c ? getThemeColor(c) : Colors.Text,
				},
				style,
			]}
			{...rest}
		/>
	);
};
