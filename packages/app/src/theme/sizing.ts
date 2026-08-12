export type ThemeIconSize = keyof typeof IconSize;
export const IconSize = {
	xs: 16,
	sm: 20,
	md: 24,
	lg: 28,
	xl: 32,
} as const;

export type ThemeControlHeight = keyof typeof ControlHeight;
export const ControlHeight = {
	sm: 32,
	md: 40,
	lg: 48,
} as const;

export type ThemeFont = keyof typeof Font;
export const Font = {
	Default: "Lexend_400Regular",
} as const;

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
	Default: 10,
	xs: 6,
	sm: 10,
	md: 12,
	lg: 16,
	xl: 20,
} as const;
