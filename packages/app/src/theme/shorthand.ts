import type { DimensionValue, ViewStyle } from "react-native";
import { Spacing as SpacingVals, type ThemeSpacing } from "./spacing";
import { resolveColor } from "./colors";

export type Spacing = ThemeSpacing | DimensionValue;

const resolveSpacing = (v: Spacing): DimensionValue =>
	SpacingVals[v as ThemeSpacing] ?? v;

/** Shorthand layout props that map directly to ViewStyle properties. */
export interface ShorthandStyleProps {
	w?: DimensionValue;
	h?: DimensionValue;
	miw?: DimensionValue;
	maw?: DimensionValue;
	mih?: DimensionValue;
	mah?: DimensionValue;
	aspectRatio?: ViewStyle["aspectRatio"];

	pos?: ViewStyle["position"];
	top?: DimensionValue;
	right?: DimensionValue;
	bottom?: DimensionValue;
	left?: DimensionValue;

	p?: Spacing;
	px?: Spacing;
	py?: Spacing;
	pt?: Spacing;
	pr?: Spacing;
	pb?: Spacing;
	pl?: Spacing;

	m?: Spacing;
	mx?: Spacing;
	my?: Spacing;
	mt?: Spacing;
	mr?: Spacing;
	mb?: Spacing;
	ml?: Spacing;

	bg?: string;
	op?: number;
	radius?: number;
	rtl?: number;
	rtr?: number;
	rbl?: number;
	rbr?: number;

	gap?: Spacing;
	rowGap?: Spacing;
	columnGap?: Spacing;

	flex?: ViewStyle["flex"];
	flexGrow?: ViewStyle["flexGrow"];
	flexShrink?: ViewStyle["flexShrink"];
	direction?: ViewStyle["flexDirection"];
	align?: ViewStyle["alignItems"];
	justify?: ViewStyle["justifyContent"];
	wrap?: ViewStyle["flexWrap"];
}

type Entry = readonly [
	prop: keyof ShorthandStyleProps,
	style: keyof ViewStyle,
	resolve?: (v: any) => any,
];

const MAP: Entry[] = [
	// Dimensions
	["w", "width"], ["h", "height"],
	["miw", "minWidth"], ["maw", "maxWidth"], ["mih", "minHeight"], ["mah", "maxHeight"],
	["aspectRatio", "aspectRatio"],
	["pos", "position"], ["top", "top"], ["right", "right"], ["bottom", "bottom"], ["left", "left"],

	// Padding, margin, gap (resolve named spacing tokens)
	["p", "padding", resolveSpacing],
	["px", "paddingHorizontal", resolveSpacing],
	["py", "paddingVertical", resolveSpacing],
	["pt", "paddingTop", resolveSpacing],
	["pr", "paddingRight", resolveSpacing],
	["pb", "paddingBottom", resolveSpacing],
	["pl", "paddingLeft", resolveSpacing],
	["m", "margin", resolveSpacing],
	["mx", "marginHorizontal", resolveSpacing],
	["my", "marginVertical", resolveSpacing],
	["mt", "marginTop", resolveSpacing],
	["mr", "marginRight", resolveSpacing],
	["mb", "marginBottom", resolveSpacing],
	["ml", "marginLeft", resolveSpacing],
	["gap", "gap", resolveSpacing],
	["rowGap", "rowGap", resolveSpacing],
	["columnGap", "columnGap", resolveSpacing],

	// Colors & radii
	["bg", "backgroundColor", resolveColor],
	["op", "opacity"],
	["radius", "borderRadius"],
	["rtl", "borderTopLeftRadius"], ["rtr", "borderTopRightRadius"],
	["rbl", "borderBottomLeftRadius"], ["rbr", "borderBottomRightRadius"],

	// Flexbox
	["flex", "flex"], ["flexGrow", "flexGrow"], ["flexShrink", "flexShrink"],
	["direction", "flexDirection"], ["align", "alignItems"],
	["justify", "justifyContent"], ["wrap", "flexWrap"],
];

/** Convert shorthand layout props to a React Native ViewStyle object. */
export const resolveShorthand = (props: ShorthandStyleProps): ViewStyle => {
	const style: ViewStyle = {};

	for (const [prop, styleKey, resolve] of MAP) {
		const value = props[prop];
		if (value === undefined) continue;
		(style as any)[styleKey] = resolve ? resolve(value) : value;
	}

	return style;
};
