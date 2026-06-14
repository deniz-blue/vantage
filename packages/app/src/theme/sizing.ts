/**
 * Sizing constants — component dimensions, font sizes, icon sizes, and gaps.
 *
 * The size tiers (xs–xl) form the canonical scale for component heights,
 * icon button sizes, and button padding. Component-specific overrides
 * (InputHeight, SurfacePadding, etc.) derive from this scale when possible.
 */

export const Sizing = {
	// === Size tiers (component heights / icon button sizes) ===
	xs: 24,
	sm: 32,
	md: 40,
	lg: 48,
	xl: 56,

	// === Radius ===
	radiusSm: 8,
	radiusMd: 10,
	radiusLg: 12,

	// === Font sizes ===
	fontSizeSm: 13,
	fontSizeMd: 15,
	fontSizeLg: 17,

	// === Icon sizes inside buttons / action icons ===
	iconSm: 16,
	iconMd: 18,

	// === Button padding per tier ===
	buttonPaddingV: { xs: 4, sm: 6, md: 10, lg: 14, xl: 18 } as const,
	buttonPaddingH: { xs: 8, sm: 12, md: 16, lg: 20, xl: 24 } as const,

	// === Base TextInput ===
	inputPaddingV: 12,
	inputPaddingH: 14,
	inputFontSize: 17,

	// === PartialDateInput component-specific overrides ===
	InputHeight: 36,
	InputFontSize: 15,
	InputPaddingV: 6,
	InputWidthYear: 72,
	InputWidthSegment: 52,
	BoxPaddingV: 2,

	// === Surface ===
	SurfacePadding: 10,
	SurfaceGap: 10,

	// === Gaps ===
	GapRow: 4,
	GapActions: 12,
} as const;
