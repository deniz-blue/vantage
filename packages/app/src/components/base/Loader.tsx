import { ActivityIndicator, type ActivityIndicatorProps } from "react-native";
import { Colors } from "../../theme/colors";

export interface LoaderProps extends ActivityIndicatorProps {
	/** @default Colors.Primary */
	color?: string;
	/** @default "large" */
	size?: "small" | "large";
}

export const Loader = ({ color = Colors.Primary, size = "large", ...rest }: LoaderProps) => (
	<ActivityIndicator size={size} color={color} {...rest} />
);
