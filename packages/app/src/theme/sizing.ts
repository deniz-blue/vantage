export type ThemeIconSize = keyof typeof IconSize;

export const IconSize = {
	xs: 16,
	sm: 20,
	md: 24,
	lg: 28,
	xl: 32,
} as const;

export const ControlHeight = {
	sm: 28,
	md: 34,
	lg: 42,
} as const;
export type ThemeControlHeight = keyof typeof ControlHeight;

export type ThemeFontSize = keyof typeof FontSize;
export const FontSize = {
	xs: 12,
	sm: 14,
	md: 16,
	lg: 18,
	xl: 20,
	h1: 24,
} as const;

export type ThemeRadius = keyof typeof Radius;
export const Radius = {
	Default: 8,
	xs: 4,
	sm: 8,
	md: 10,
	lg: 12,
	xl: 16,
} as const;
