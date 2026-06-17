export type ThemeIconSize = keyof typeof IconSize;

export const IconSize = {
	sm: 20,
	md: 24,
	lg: 28,
} as const;

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
	sm: 8,
	md: 10,
	lg: 12,
	xl: 16,
} as const;

export const Sizing = {
	xs: 24,
	sm: 32,
	md: 40,
	lg: 48,
	xl: 56,
} as const;
