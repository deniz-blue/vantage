import type { PropsWithChildren } from "react";
import { ErrorBoundary } from "../base/ErrorBoundary";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Button } from "../base/button/Button";
import { Colors } from "../../theme/colors";
import { FontSize, Radius } from "../../theme/sizing";
import { Spacing } from "../../theme/spacing";

export const AppErrorFallback = ({ error, reset }: { error: Error; reset: () => void }) => (
	<Box
		flex={1}
		bg={Colors.Background}
		p={Spacing.lg}
		justify="center"
		align="center"
		gap={Spacing.md}
	>
		<Box
			bg={Colors.BackgroundLight}
			radius={Radius.lg}
			p={Spacing.lg}
			style={{ borderWidth: 1, borderColor: Colors.Border, maxWidth: 400, width: "100%" }}
			gap={Spacing.md}
		>
			<Text fz={FontSize.h1} fw="bold" c={Colors.Red7}>
				Something went wrong
			</Text>
			<Text fz={FontSize.sm} c={Colors.TextDimmed}>
				{error.message || "An unexpected error occurred."}
			</Text>
			<Button variant="primary" onPress={reset}>
				Try again
			</Button>
		</Box>
	</Box>
);

export const AppErrorBoundary = ({ children }: PropsWithChildren) => (
	<ErrorBoundary
		fallback={(error, reset) => <AppErrorFallback error={error} reset={reset} />}
		onError={(error, info) => console.error("AppErrorBoundary:", error, info.componentStack)}
	>
		{children}
	</ErrorBoundary>
);
