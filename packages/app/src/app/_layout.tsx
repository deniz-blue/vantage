import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { queryClient } from "@vantage/core";
import { Box } from "../components/base/Box";
import { Colors } from "../theme/colors";
import { ComponentStack } from "../components/ComponentStack";
import { PortalHost, PortalProvider } from "react-native-teleport";
import { FiberHandle } from "../internal/react-context-bridge";
import { BottomSheetModalProvider } from "@gorhom/bottom-sheet";

export default function RootLayout() {
	useEffect(() => {
		SplashScreen.hideAsync();
	}, []);

	return (
		<ComponentStack
			stack={[
				[GestureHandlerRootView, {}],
				[BottomSheetModalProvider, {}],
				[FiberHandle, {}],
				[PortalProvider, {}],
				[QueryClientProvider, { client: queryClient }],
			]}
		>
			<PortalHost name="overlay" />
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
