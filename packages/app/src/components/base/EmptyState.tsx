import type { ReactNode } from "react";
import { Box } from "./Box";
import { Text } from "./Text";
import { Loader } from "./Loader";
import { Colors } from "../../theme/colors";

export interface EmptyStateProps {
	/** Show loading spinner instead of icon. */
	loading?: boolean;
	/** Optional icon component shown when not loading. */
	icon?: ReactNode;
	/** Message below the spinner/icon. */
	message?: string;
}

export const EmptyState = ({
	loading = true,
	icon,
	message,
}: EmptyStateProps) => (
	<Box flex={1} justify="center" align="center" gap="md" p="lg">
		{loading ? <Loader /> : icon}
		{message && (
			<Text style={{ color: Colors.TextDimmed, textAlign: "center" }}>
				{message}
			</Text>
		)}
	</Box>
);
