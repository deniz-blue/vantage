import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";
import { Box } from "../components/Box";
import { Colors } from "../theme/colors";

export default function RootLayout() {
	useEffect(() => {
		SplashScreen.hideAsync();
	}, []);

	return (
		<Box
			style={{
				flex: 1,
				backgroundColor: Colors.Background,
			}}
		>
			<Stack>
				<Stack.Screen
					name="(tabs)"
					options={{ headerShown: false }}
				/>
			</Stack>
		</Box>
	);
}
