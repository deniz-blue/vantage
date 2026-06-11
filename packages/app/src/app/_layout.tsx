import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { queryClient } from "@vantage/core";
import { Box } from "../components/base/Box";
import { Colors } from "../theme/colors";

export default function RootLayout() {
	useEffect(() => {
		SplashScreen.hideAsync();
	}, []);

	return (
		<QueryClientProvider client={queryClient}>
			<Box
				style={{
					flex: 1,
					backgroundColor: Colors.Background,
				}}
			>
				<Stack screenOptions={{
						contentStyle: { backgroundColor: Colors.Background },
					}}>
					<Stack.Screen
						name="(tabs)"
						options={{ headerShown: false }}
					/>
				</Stack>
			</Box>
		</QueryClientProvider>
	);
}
