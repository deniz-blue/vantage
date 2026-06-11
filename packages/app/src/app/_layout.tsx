import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect, useState } from "react";
import * as ExpoLexendFonts from "@expo-google-fonts/lexend";
import { Box } from "../components/Box";
import { Colors } from "../theme/colors";
import { initDb } from "../lib/db-init";

SplashScreen.preventAutoHideAsync();

const { useFonts, ...fonts } = ExpoLexendFonts;

export default function RootLayout() {
	const [loaded] = useFonts(fonts);
	const [dbReady, setDbReady] = useState(false);

	useEffect(() => {
		initDb().then(() => setDbReady(true));
	}, []);

	useEffect(() => {
		if (loaded && dbReady) SplashScreen.hide();
	}, [loaded, dbReady]);

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
