import { Radius } from "./sizing";

export type ThemeSpacing = keyof typeof Spacing;
export const Spacing = {
	xs: 4,
	sm: 8,
	md: 16,
	lg: 24,
	xl: 32,

	Radius: Radius.Default,
} as const;

/** @deprecated */
export type SpacingName = keyof typeof Spacing;
