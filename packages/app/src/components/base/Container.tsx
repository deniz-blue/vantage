import { Box, type BoxProps } from "./Box";

const CONTAINER_WIDTHS = {
	sm: 640,
	md: 768,
	lg: 1024,
	xl: 1200,
} as const;

type ContainerSize = keyof typeof CONTAINER_WIDTHS;

export interface ContainerProps extends BoxProps {
	size?: ContainerSize;
	maxWidth?: number;
}

export const Container = ({ children, size = "md", maxWidth, style, ...rest }: ContainerProps) => (
	<Box
		w="100%"
		px="md"
		style={[
			{
				alignSelf: "center",
				maxWidth: maxWidth ?? CONTAINER_WIDTHS[size],
			},
			style,
		]}
		{...rest}
	>
		{children}
	</Box>
);
