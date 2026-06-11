import { DimensionValue, ViewStyle, View, ViewProps } from "react-native";

import { Spacing as SpacingVals, type SpacingName } from "../../theme/spacing";

export type Spacing = SpacingName | DimensionValue;

const resolve = (v: Spacing | undefined): DimensionValue | undefined =>
	v !== undefined ? (SpacingVals[v as SpacingName] ?? v) : undefined;

// === Props ===

export interface BoxProps extends ViewProps {
	w?: DimensionValue;
	h?: DimensionValue;
	miw?: DimensionValue;
	maw?: DimensionValue;
	mih?: DimensionValue;
	mah?: DimensionValue;

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
	direction?: ViewStyle["flexDirection"];
	align?: ViewStyle["alignItems"];
	justify?: ViewStyle["justifyContent"];
	wrap?: ViewStyle["flexWrap"];
}

// === Component ===

export const Box = (props: BoxProps) => {
	const style: ViewStyle = {};

	const dir = { l: "Left", r: "Right", t: "Top", b: "Bottom", x: "Horizontal", y: "Vertical" } as const;

	for (const key in props) switch (true) {
		case key == "w": style.width = props.w as any; break;
		case key == "h": style.height = props.h as any; break;
		case key == "miw": style.minWidth = props.miw as any; break;
		case key == "maw": style.maxWidth = props.maw as any; break;
		case key == "mih": style.minHeight = props.mih as any; break;
		case key == "mah": style.maxHeight = props.mah as any; break;

		case key == "pos": style.position = props.pos as any; break;
		case key == "top": style.top = props.top as any; break;
		case key == "right": style.right = props.right as any; break;
		case key == "bottom": style.bottom = props.bottom as any; break;
		case key == "left": style.left = props.left as any; break;

		case key[0] == "p": {
			const suffix = key.slice(1) as keyof typeof dir;
			if (dir[suffix]) (style as any)[`padding${dir[suffix]}`] = resolve(props[key as keyof BoxProps] as Spacing);
			else if (key == "p") style.padding = resolve(props.p);
		} break;

		case key[0] == "m": {
			const suffix = key.slice(1) as keyof typeof dir;
			if (dir[suffix]) (style as any)[`margin${dir[suffix]}`] = resolve(props[key as keyof BoxProps] as Spacing);
			else if (key == "m") style.margin = resolve(props.m);
		} break;

		case key == "bg": style.backgroundColor = props.bg as any; break;
		case key == "op": style.opacity = props.op as any; break;
		case key == "radius": style.borderRadius = props.radius as any; break;
		case key == "rtl": style.borderTopLeftRadius = props.rtl as any; break;
		case key == "rtr": style.borderTopRightRadius = props.rtr as any; break;
		case key == "rbl": style.borderBottomLeftRadius = props.rbl as any; break;
		case key == "rbr": style.borderBottomRightRadius = props.rbr as any; break;

		case key == "gap": style.gap = resolve(props.gap) as any; break;
		case key == "rowGap": style.rowGap = resolve(props.rowGap) as any; break;
		case key == "columnGap": style.columnGap = resolve(props.columnGap) as any; break;

		case key == "flex": style.flex = props.flex as any; break;
		case key == "direction": style.flexDirection = props.direction as any; break;
		case key == "align": style.alignItems = props.align as any; break;
		case key == "justify": style.justifyContent = props.justify as any; break;
		case key == "wrap": style.flexWrap = props.wrap as any; break;
	}

	const { 
		w, h, miw, maw, mih, mah,
		pos, top, right, bottom, left,
		p, px, py, pt, pr, pb, pl,
		m, mx, my, mt, mr, mb, ml,
		bg, op, radius, rtl, rtr, rbl, rbr,
		gap, rowGap, columnGap,
		flex, direction, align, justify, wrap,
		...rest
	} = props;

	return (
		<View
			style={[style, props.style]}
			{...rest}
		/>
	);
};
