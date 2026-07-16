import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { queryClient } from "@vantage/core";
import { Box } from "../components/base/Box";
import { Colors } from "../theme/colors";
import { ComponentStack } from "../components/ComponentStack";
import { initializeDatabase } from "@vantage/db";
import { BottomSheetProvider } from "@swmansion/react-native-bottom-sheet";
import { KeyboardProvider } from "react-native-keyboard-controller";

export default function RootLayout() {
	console.log("RootLayout render");
	const [ready, setReady] = useState(false);

	useEffect(() => {
		(async () => {
			console.log("Initializing database...");
			await initializeDatabase();
			await SplashScreen.hideAsync();
			setReady(true);
			console.log("Database initialized!");
		})().catch((err) => {
			console.error("Error initializing database:", err);
		});
	}, []);

	if (!ready) return null;

	return (
		<ComponentStack
			stack={[
				[KeyboardProvider, {}],
				[GestureHandlerRootView, {}],
				[BottomSheetProvider, {}],
				[QueryClientProvider, { client: queryClient }],
			]}
		>
			<Box flex={1} bg={Colors.Background}>
				<Stack
					screenOptions={{
						contentStyle: { backgroundColor: Colors.Background },
					}}
				>
					<Stack.Screen name="(tabs)" options={{ headerShown: false }} />
					<Stack.Screen name="event/index" options={{ headerShown: false }} />
					<Stack.Screen name="event/[id]" options={{ headerShown: false }} />
					<Stack.Screen name="event/[id]/edit" options={{ headerShown: false }} />
				</Stack>
			</Box>
		</ComponentStack>
	);
}
