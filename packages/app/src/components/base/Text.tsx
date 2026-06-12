import type { DimensionValue, TextStyle } from "react-native";
import { TextProps as RNTextProps, Text as RNText } from "react-native";
import { Colors, getThemeColor, type ThemeColor } from "../../theme/colors";
import { Spacing, type SpacingName } from "../../theme/spacing";

type Spacing = SpacingName | DimensionValue;

const resolveSpacing = (v: Spacing | undefined): DimensionValue | undefined =>
	v !== undefined ? (Spacing[v as SpacingName] ?? v) : undefined;

export interface TextProps extends RNTextProps {
	c?: ThemeColor;
	fz?: number;
	fw?: TextStyle["fontWeight"];
	ta?: TextStyle["textAlign"];
	tt?: TextStyle["textTransform"];
	lh?: number;
	mt?: Spacing;
	mb?: Spacing;
	ml?: Spacing;
	mr?: Spacing;
	mx?: Spacing;
	my?: Spacing;
}

export const Text = (props: TextProps) => {
	const {
		c,
		fz,
		fw,
		ta,
		tt,
		lh,
		mt,
		mb,
		ml,
		mr,
		mx,
		my,
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
					textAlign: ta,
					textTransform: tt,
					lineHeight: lh,
					marginTop: resolveSpacing(mt),
					marginBottom: resolveSpacing(mb),
					marginLeft: resolveSpacing(ml ?? mx),
					marginRight: resolveSpacing(mr ?? mx),
				},
				style,
			]}
			{...rest}
		/>
	);
};
