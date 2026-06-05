import { FlexStyle, View, ViewProps } from "react-native";
import { getThemeSpacing, Spacing, ThemeSpacing } from "../../theme/spacing";
import { getThemeColor, ThemeColor } from "../../theme/colors";

type Dir = "t" | "r" | "b" | "l" | "x" | "y";
type DirKey = `p${Dir}` | `m${Dir}` | "p" | "m";
type ThemeSpacingProps = {
	[key in DirKey]?: ThemeSpacing;
};

export interface BoxProps extends ViewProps, ThemeSpacingProps {
	direction?: "row" | "column";
	gap?: ThemeSpacing;
	bg?: ThemeColor;
	bdc?: ThemeColor;
	bdr?: ThemeSpacing;
	w?: FlexStyle["width"];
	h?: FlexStyle["height"];
	align?: FlexStyle["alignItems"];
	justify?: FlexStyle["justifyContent"];
};

export const Box = (props: BoxProps) => {
	const style: ViewProps["style"] = {};

	if (props.bg) style.backgroundColor = getThemeColor(props.bg);
	if (props.bdc) style.borderColor = getThemeColor(props.bdc);
	if (props.bdr) style.borderRadius = getThemeSpacing(props.bdr);
	if (props.bdr || props.bdc) style.borderWidth = Spacing.bdw;
	if (props.direction) style.flexDirection = props.direction;
	if (props.gap) style.gap = getThemeSpacing(props.gap);
	if (props.w) style.width = props.w;
	if (props.h) style.height = props.h;
	if (props.align) style.alignItems = props.align;
	if (props.justify) style.justifyContent = props.justify;

	for (const key in props) {
		if ((key[0] === "p" || key[0] === "m") && key.length <= 2) {
			const ty = key[0] as "p" | "m";
			const prefix = ty === "p" ? "padding" : "margin";
			const dir = key.slice(1) as Dir | undefined;
			const value = props[key as keyof BoxProps] as ThemeSpacing | undefined;
			const suffix = ({
				t: "Top",
				r: "Right",
				b: "Bottom",
				l: "Left",
				x: "Horizontal",
				y: "Vertical",
				"": "",
			} as const)[dir || ""] || "";
			style[`${prefix}${suffix}`] = getThemeSpacing(value || 0);
		}
	}

	return (
		<View
			{...props}
			style={[style, props.style]}
		/>
	);
};
