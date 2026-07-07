import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { queryClient } from "@vantage/core";
import { Box } from "../components/base/Box";
import { Colors } from "../theme/colors";
import { BottomSheetHost, BottomSheetManagerProvider, BottomSheetScaleView } from "react-native-bottom-sheet-stack";
import { ComponentStack } from "../components/ComponentStack";

export default function RootLayout() {
	useEffect(() => {
		SplashScreen.hideAsync();
	}, []);

	return (
		<ComponentStack
			stack={[
				[BottomSheetManagerProvider, { id: "default" }],
				[QueryClientProvider, { client: queryClient }],
				[GestureHandlerRootView, {}],
				[BottomSheetScaleView, {}],
			]}
		>
			<BottomSheetHost />
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
