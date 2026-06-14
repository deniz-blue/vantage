import { type ComponentPropsWithoutRef, type ElementType } from "react";
import { View } from "react-native";

import { type ShorthandStyleProps, resolveShorthand } from "../../theme/shorthand";

export type { ShorthandStyleProps, Spacing } from "../../theme/shorthand";

export type BoxProps<T extends ElementType = typeof View> =
	ShorthandStyleProps &
	Omit<ComponentPropsWithoutRef<T>, keyof ShorthandStyleProps | "style"> & {
		component?: T;
		style?: ComponentPropsWithoutRef<typeof View>["style"];
	};

export const Box = <T extends ElementType = typeof View>(props: BoxProps<T>) => {
	const {
		component: Component = View as unknown as T,
		...rest
	} = props as any;

	const {
		w, h, miw, maw, mih, mah,
		pos, top, right, bottom, left,
		p, px, py, pt, pr, pb, pl,
		m, mx, my, mt, mr, mb, ml,
		bg, op, radius, rtl, rtr, rbl, rbr,
		gap, rowGap, columnGap,
		flex, direction, align, justify, wrap,
		style,
		...passthrough
	} = rest;

	const shorthand = {
		w, h, miw, maw, mih, mah,
		pos, top, right, bottom, left,
		p, px, py, pt, pr, pb, pl,
		m, mx, my, mt, mr, mb, ml,
		bg, op, radius, rtl, rtr, rbl, rbr,
		gap, rowGap, columnGap,
		flex, direction, align, justify, wrap,
	};

	return (
		<Component
			style={[resolveShorthand(shorthand), style]}
			{...passthrough}
		/>
	);
};
