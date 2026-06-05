const Pre = {
	xs: 2,
	sm: 4,
	md: 8,
	lg: 16,
	xl: 32,
};

export const Spacing = {
	...Pre,
	none: 0,
	r: Pre.md,
	bdw: 2,
};

export type ThemeSpacing = keyof typeof Spacing | number;
export const getThemeSpacing = (value: ThemeSpacing) => {
	if (typeof value === "number") return value;
	return Spacing[value as keyof typeof Spacing] || 0;
};
