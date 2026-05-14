import { createTheme, DEFAULT_THEME, Modal, type ActionIcon, type Button, type Tooltip } from "@mantine/core";

export const theme = createTheme({
	fontFamily: "Lexend, " + DEFAULT_THEME.fontFamily,
	components: {
		ActionIcon: {
			defaultProps: {
				variant: "light",
			} as ActionIcon.Props,
		},
		Button: {
			defaultProps: {
				variant: "light",
			} as Button.Props,
		},
		Tooltip: {
			defaultProps: {
				withArrow: true,
				arrowOffset: 4,
				arrowSize: 6,
				color: "gray",
				opacity: 0.9,
				multiline: true,
			} as Tooltip.Props,
		},
		Modal: {
			defaultProps: {
				centered: true,
				size: "lg",
				withCloseButton: false,
			} as Modal.Props,
		},
	},
});
