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
import { StyleSheet } from "react-native";

export default function RootLayout() {
	useEffect(() => {
		SplashScreen.hideAsync();
	}, []);

	return (
		<ComponentStack
			stack={[
				[QueryClientProvider, { client: queryClient }],
				[GestureHandlerRootView, {}],
				[PortalProvider, {}],
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
			<PortalHost
				name="overlay"
				style={{
					position: "absolute",
					top: 0,
					right: 0,
					bottom: 0,
					left: 0,
					zIndex: 67,
				}}
			/>
		</ComponentStack>
	);
}
