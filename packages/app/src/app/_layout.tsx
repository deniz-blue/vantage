import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect, useState } from "react";
import * as ExpoLexendFonts from "@expo-google-fonts/lexend";
import { queryClient } from "@vantage/core";
import { QueryClientProvider } from "@tanstack/react-query";
import { initDb } from "../init-db";

SplashScreen.preventAutoHideAsync();

const { useFonts, ...fonts } = ExpoLexendFonts;

export default function RootLayout() {
	const [fontsLoaded] = useFonts(fonts);
	const [dbReady, setDbReady] = useState(false);

	useEffect(() => {
		(async () => {
			await initDb();
			setDbReady(true);
		})();
	}, []);

	const isReady = fontsLoaded && dbReady;

	useEffect(() => {
		if (isReady) SplashScreen.hide();
	}, [isReady]);

	if (!isReady) return null;

	return (
		<QueryClientProvider client={queryClient}>
			<Stack>
				<Stack.Screen
					name="(tabs)"
					options={{ headerShown: false }}
				/>
			</Stack>
		</QueryClientProvider>
	);
}
