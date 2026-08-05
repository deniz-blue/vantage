import type { ReactNode } from "react";
import { Box } from "./Box";
import { Text } from "./Text";
import { Loader } from "./Loader";
import { Colors } from "../../theme/colors";

export interface EmptyStateProps {
	loading?: boolean;
	icon?: ReactNode;
	message?: string;
	fill?: boolean;
	action?: ReactNode;
}

export const EmptyState = ({ loading, icon, message, fill, action }: EmptyStateProps) => (
	<Box flex={fill ? 1 : undefined} justify="center" align="center" gap="md" p="lg">
		{loading ? <Loader /> : icon}
		{message && <Text style={{ color: Colors.TextDimmed, textAlign: "center" }}>{message}</Text>}
		{action}
	</Box>
);
