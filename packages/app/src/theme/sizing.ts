export type ThemeFontSize = keyof typeof FontSize;
export const FontSize = {
	xs: 12,
	sm: 14,
	md: 16,
	lg: 18,
	xl: 20,
	h1: 24,
};

export type ThemeRadius = keyof typeof Radius;
export const Radius = {
	Default: 8,
};

export const Sizing = {
	xs: 24,
	sm: 32,
	md: 40,
	lg: 48,
	xl: 56,

	radiusSm: 8,
	radiusMd: 10,
	radiusLg: 12,
	radiusXl: 16,

	/** @deprecated */
	fontSizeSm: FontSize.sm,
	/** @deprecated */
	fontSizeMd: FontSize.md,
	/** @deprecated */
	fontSizeLg: FontSize.lg,

	// === Icon sizes inside buttons / action icons ===
	iconSm: 16,
	iconMd: 18,

	// === Button padding per tier ===
	buttonPaddingV: { xs: 4, sm: 6, md: 10, lg: 14, xl: 18 } as const,
	buttonPaddingH: { xs: 8, sm: 12, md: 16, lg: 20, xl: 24 } as const,

} as const;
