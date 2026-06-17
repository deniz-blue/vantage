import type { DimensionValue, ViewStyle } from "react-native";
import { Spacing as SpacingVals, type SpacingName } from "./spacing";
import { resolveColor } from "./colors";

export type Spacing = SpacingName | DimensionValue;

const resolve = (v: Spacing | undefined): DimensionValue | undefined =>
	v !== undefined ? (SpacingVals[v as SpacingName] ?? v) : undefined;

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

/** Convert shorthand layout props to a React Native ViewStyle object. */
export const resolveShorthand = (props: ShorthandStyleProps): ViewStyle => {
	const style: ViewStyle = {};

	if (props.w !== undefined) style.width = props.w;
	if (props.h !== undefined) style.height = props.h;
	if (props.miw !== undefined) style.minWidth = props.miw;
	if (props.maw !== undefined) style.maxWidth = props.maw;
	if (props.mih !== undefined) style.minHeight = props.mih;
	if (props.mah !== undefined) style.maxHeight = props.mah;
	if (props.aspectRatio !== undefined) style.aspectRatio = props.aspectRatio;

	if (props.pos !== undefined) style.position = props.pos;
	if (props.top !== undefined) style.top = props.top;
	if (props.right !== undefined) style.right = props.right;
	if (props.bottom !== undefined) style.bottom = props.bottom;
	if (props.left !== undefined) style.left = props.left;

	if (props.p !== undefined) style.padding = resolve(props.p);
	if (props.px !== undefined) { const v = resolve(props.px); style.paddingHorizontal = v; }
	if (props.py !== undefined) { const v = resolve(props.py); style.paddingVertical = v; }
	if (props.pt !== undefined) style.paddingTop = resolve(props.pt);
	if (props.pr !== undefined) style.paddingRight = resolve(props.pr);
	if (props.pb !== undefined) style.paddingBottom = resolve(props.pb);
	if (props.pl !== undefined) style.paddingLeft = resolve(props.pl);

	if (props.m !== undefined) style.margin = resolve(props.m);
	if (props.mx !== undefined) { const v = resolve(props.mx); style.marginHorizontal = v; }
	if (props.my !== undefined) { const v = resolve(props.my); style.marginVertical = v; }
	if (props.mt !== undefined) style.marginTop = resolve(props.mt);
	if (props.mr !== undefined) style.marginRight = resolve(props.mr);
	if (props.mb !== undefined) style.marginBottom = resolve(props.mb);
	if (props.ml !== undefined) style.marginLeft = resolve(props.ml);

	if (props.bg !== undefined) style.backgroundColor = resolveColor(props.bg);
	if (props.op !== undefined) style.opacity = props.op;
	if (props.radius !== undefined) style.borderRadius = props.radius;
	if (props.rtl !== undefined) style.borderTopLeftRadius = props.rtl;
	if (props.rtr !== undefined) style.borderTopRightRadius = props.rtr;
	if (props.rbl !== undefined) style.borderBottomLeftRadius = props.rbl;
	if (props.rbr !== undefined) style.borderBottomRightRadius = props.rbr;

	if (props.gap !== undefined) style.gap = resolve(props.gap) as number;
	if (props.rowGap !== undefined) style.rowGap = resolve(props.rowGap) as number;
	if (props.columnGap !== undefined) style.columnGap = resolve(props.columnGap) as number;

	if (props.flex !== undefined) style.flex = props.flex;
	if (props.flexGrow !== undefined) style.flexGrow = props.flexGrow;
	if (props.flexShrink !== undefined) style.flexShrink = props.flexShrink;
	if (props.direction !== undefined) style.flexDirection = props.direction;
	if (props.align !== undefined) style.alignItems = props.align;
	if (props.justify !== undefined) style.justifyContent = props.justify;
	if (props.wrap !== undefined) style.flexWrap = props.wrap;

	return style;
};
