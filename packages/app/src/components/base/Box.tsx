import { DimensionValue, ViewStyle, View, ViewProps } from "react-native";

import { Spacing as SpacingVals, type SpacingName } from "../../theme/spacing";
import { resolveColor } from "../../theme/colors";

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
	const computed: ViewStyle = {};

	const dir = { l: "Left", r: "Right", t: "Top", b: "Bottom", x: "Horizontal", y: "Vertical" } as const;

	for (const key in props) switch (true) {
		case key == "w": computed.width = props.w as any; break;
		case key == "h": computed.height = props.h as any; break;
		case key == "miw": computed.minWidth = props.miw as any; break;
		case key == "maw": computed.maxWidth = props.maw as any; break;
		case key == "mih": computed.minHeight = props.mih as any; break;
		case key == "mah": computed.maxHeight = props.mah as any; break;

		case key == "pos": computed.position = props.pos as any; break;
		case key == "top": computed.top = props.top as any; break;
		case key == "right": computed.right = props.right as any; break;
		case key == "bottom": computed.bottom = props.bottom as any; break;
		case key == "left": computed.left = props.left as any; break;

		case key[0] == "p": {
			const suffix = key.slice(1) as keyof typeof dir;
			if (dir[suffix]) (computed as any)[`padding${dir[suffix]}`] = resolve(props[key as keyof BoxProps] as Spacing);
			else if (key == "p") computed.padding = resolve(props.p);
		} break;

		case key[0] == "m": {
			const suffix = key.slice(1) as keyof typeof dir;
			if (dir[suffix]) (computed as any)[`margin${dir[suffix]}`] = resolve(props[key as keyof BoxProps] as Spacing);
			else if (key == "m") computed.margin = resolve(props.m);
		} break;

		case key == "bg": computed.backgroundColor = props.bg ? resolveColor(props.bg as string) : undefined; break;
		case key == "op": computed.opacity = props.op as any; break;
		case key == "radius": computed.borderRadius = props.radius as any; break;
		case key == "rtl": computed.borderTopLeftRadius = props.rtl as any; break;
		case key == "rtr": computed.borderTopRightRadius = props.rtr as any; break;
		case key == "rbl": computed.borderBottomLeftRadius = props.rbl as any; break;
		case key == "rbr": computed.borderBottomRightRadius = props.rbr as any; break;

		case key == "gap": computed.gap = resolve(props.gap) as any; break;
		case key == "rowGap": computed.rowGap = resolve(props.rowGap) as any; break;
		case key == "columnGap": computed.columnGap = resolve(props.columnGap) as any; break;

		case key == "flex": computed.flex = props.flex as any; break;
		case key == "direction": computed.flexDirection = props.direction as any; break;
		case key == "align": computed.alignItems = props.align as any; break;
		case key == "justify": computed.justifyContent = props.justify as any; break;
		case key == "wrap": computed.flexWrap = props.wrap as any; break;
	}

	const { 
		w, h, miw, maw, mih, mah,
		pos, top, right, bottom, left,
		p, px, py, pt, pr, pb, pl,
		m, mx, my, mt, mr, mb, ml,
		bg, op, radius, rtl, rtr, rbl, rbr,
		gap, rowGap, columnGap,
		flex, direction, align, justify, wrap,
		style,
		...rest
	} = props;

	return (
		<View
			style={[computed, style]}
			{...rest}
		/>
	);
};
