import { Colors } from "../../theme/colors";
import { Box, type Spacing } from "./Box";

export interface DividerProps {
	color?: string;
	thickness?: number;
	/** @default false — horizontal line */
	vertical?: boolean;
	/** Margin around the divider. */
	my?: Spacing;
	mx?: Spacing;
}

export const Divider = ({
	color = Colors.BackgroundLight,
	thickness = 1,
	vertical = false,
	my,
	mx,
}: DividerProps) => (
	<Box
		{...(vertical
			? { w: thickness, h: "100%", mx: mx ?? "sm" }
			: { h: thickness, w: "100%", my: my ?? "sm" }
		)}
		bg={color}
	/>
);
