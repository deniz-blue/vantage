import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { BottomSheetModalProvider } from "@gorhom/bottom-sheet";
import { queryClient } from "@vantage/core";
import { Box } from "../components/base/Box";
import { Colors } from "../theme/colors";
import { FiberHandle } from "../internal/react-context-bridge";

export default function RootLayout() {
	useEffect(() => {
		SplashScreen.hideAsync();
	}, []);

	return (
		<GestureHandlerRootView style={{ flex: 1 }}>
			<BottomSheetModalProvider>
				<QueryClientProvider client={queryClient}>
					<FiberHandle>
						<Box flex={1} bg={Colors.Background}>
							<Stack screenOptions={{
								contentStyle: { backgroundColor: Colors.Background },
							}}>
								<Stack.Screen
									name="(tabs)"
									options={{ headerShown: false }}
								/>
								<Stack.Screen
									name="event/[id]"
									options={{ headerShown: false }}
								/>
								<Stack.Screen
									name="event/[id]/edit"
									options={{ headerShown: false }}
								/>
							</Stack>
						</Box>
					</FiberHandle>
				</QueryClientProvider>
			</BottomSheetModalProvider>
		</GestureHandlerRootView>
	);
}
