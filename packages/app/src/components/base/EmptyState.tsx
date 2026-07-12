import type { ReactNode } from "react";
import { Box } from "./Box";
import { Text } from "./Text";
import { Loader } from "./Loader";
import { Colors } from "../../theme/colors";

export interface EmptyStateProps {
	loading?: boolean;
	icon?: ReactNode;
	message?: string;
}

export const EmptyState = ({ loading, icon, message }: EmptyStateProps) => (
	<Box flex={1} justify="center" align="center" gap="md" p="lg">
		{loading ? <Loader /> : icon}
		{message && <Text style={{ color: Colors.TextDimmed, textAlign: "center" }}>{message}</Text>}
	</Box>
);
