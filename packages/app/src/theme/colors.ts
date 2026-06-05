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
	Primary: Palette.Grape7,
	PrimaryLight: Palette.Grape6,
	Text: Palette.Dark0,
	TextDimmed: Palette.Dark5,
	Border: Palette.Dark8,
};

export type ThemeColor = keyof typeof Colors | (string & {});

export const getThemeColor = (value: ThemeColor) => {
	if (typeof value === "string") return Colors[value as keyof typeof Colors] || value;
	return Colors[value as keyof typeof Colors] || value;
};
