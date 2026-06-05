import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { useEffect } from "react";
import * as ExpoLexendFonts from "@expo-google-fonts/lexend";
import { Box } from "../components/base/Box";
import { Colors } from "../theme/colors";

SplashScreen.preventAutoHideAsync();

const { useFonts, ...fonts } = ExpoLexendFonts;

export default function RootLayout() {
	const [loaded] = useFonts(fonts);

	useEffect(() => {
		if (loaded) SplashScreen.hide();
	}, [loaded]);

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
