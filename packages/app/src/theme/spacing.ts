export const Spacing = {
	xs: 4,
	sm: 8,
	md: 16,
	lg: 24,
	xl: 32,

	/** Default border radius for inputs, cards, etc */
	Radius: 8,
} as const;

export type SpacingName = keyof typeof Spacing;
