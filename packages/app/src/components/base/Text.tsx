import type { TextStyle } from "react-native";
import { TextProps as RNTextProps, Text as RNText } from "react-native";
import { Colors, getThemeColor, type ThemeColor } from "../../theme/colors";

export interface TextProps extends RNTextProps {
	c?: ThemeColor;
	fz?: number;
	fw?: TextStyle["fontWeight"];
	fst?: TextStyle["fontStyle"];
	ta?: TextStyle["textAlign"];
	tt?: TextStyle["textTransform"];
	tdl?: TextStyle["textDecorationLine"];
	lh?: number;
}

export const Text = (props: TextProps) => {
	const {
		c,
		fz,
		fw,
		fst,
		ta,
		tt,
		tdl,
		lh,
		style,
		...rest
	} = props;

	return (
		<RNText
			style={[
				{
					fontFamily: "Lexend",
					color: c ? getThemeColor(c) : Colors.Text,
					fontSize: fz,
					fontWeight: fw,
					fontStyle: fst,
					textAlign: ta,
					textTransform: tt,
					textDecorationLine: tdl,
					lineHeight: lh,
				},
				style,
			]}
			{...rest}
		/>
	);
};
