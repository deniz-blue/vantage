const Palette = {
	Dark0: "#C9C9C9",
	Dark1: "#b8b8b8",
	Dark2: "#828282",
	Dark3: "#696969",
	Dark4: "#424242",
	Dark5: "#3b3b3b",
	Dark6: "#2e2e2e",
	Dark7: "#242424",
	Dark8: "#1f1f1f",
	Dark9: "#141414",

	Gray0: "#f8f9fa",
	Gray1: "#f1f3f5",
	Gray2: "#e9ecef",
	Gray3: "#dee2e6",
	Gray4: "#ced4da",
	Gray5: "#adb5bd",
	Gray6: "#868e96",
	Gray7: "#495057",
	Gray8: "#343a40",
	Gray9: "#212529",

	Grape6: "#be4bdb",
	Grape7: "#ae3ec9",
};

export const Colors = {
	...Palette,
	Background: Palette.Dark7,
	BackgroundLight: Palette.Dark6,
	BackgroundInput: Palette.Dark6,
	Primary: Palette.Grape7,
	PrimaryLight: Palette.Grape6,
	PrimaryTint: Palette.Grape7 + "33", // Primary at ~20% opacity
	Text: Palette.Dark0,
	TextDimmed: Palette.Dark2,
	Border: Palette.Dark8,

	// Named CSS colors
	Red: "#f44336",
	Pink: "#e91e63",
	Purple: "#9C27B0",
	DeepPurple: "#673AB7",
	Indigo: "#3F51B5",
	Blue: "#2196F3",
	LightBlue: "#03A9F4",
	Cyan: "#00BCD4",
	Teal: "#009688",
	Green: "#4CAF50",
	LightGreen: "#8BC34A",
	Lime: "#CDDC39",
	Yellow: "#FFEB3B",
	Amber: "#FFC107",
	Orange: "#FF9800",
	DeepOrange: "#FF5722",
	Brown: "#795548",
	Grey: "#9E9E9E",
	BlueGrey: "#607D8B",
	White: "#FFFFFF",
	Black: "#000000",
} as const;

export type ThemeColor = keyof typeof Colors | (string & {});

/** Resolves a color name/key to its hex value. */
export const getThemeColor = (value: ThemeColor): string => {
	return Colors[value as keyof typeof Colors] || value;
};

/** Shorthand for `getThemeColor` — resolves both `"red"` and `"Red"` etc. */
export const resolveColor = (color: string): string => {
	const key = color.charAt(0).toUpperCase() + color.slice(1) as keyof typeof Colors;
	if (key in Colors) return Colors[key]!;
	return color;
};
