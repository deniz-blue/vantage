import { useWindowDimensions } from "react-native";
import { Tabs } from "expo-router";
import { IconHome, IconList, IconSettings } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";

const WIDE_BREAKPOINT = 640;
const TAB_BAR_MAX_WIDTH = 240;

export default function TabLayout() {
	const { width: screenWidth } = useWindowDimensions();
	const isWide = screenWidth >= WIDE_BREAKPOINT;

	return (
		<Tabs
			screenOptions={{
				tabBarActiveTintColor: Colors.Primary,
				tabBarInactiveTintColor: Colors.TextDimmed,
				sceneStyle: { backgroundColor: Colors.Background },
				tabBarStyle: {
					backgroundColor: Colors.BackgroundLight,
					borderTopWidth: 0,
					elevation: 0,
					overflow: "hidden",
					...(isWide
						? {
							maxWidth: TAB_BAR_MAX_WIDTH,
							alignSelf: "center",
							marginBottom: 8,
							borderRadius: 16,
						}
						: {}),
				},
				tabBarItemStyle: {
					paddingVertical: 6,
				},
				tabBarShowLabel: false,
				headerShown: false,
			}}
		>
			<Tabs.Screen
				name="index"
				options={{
					title: "Home",
					tabBarIcon: ({ color, size }) => (
						<IconHome size={size} color={color} />
					),
				}}
			/>
			<Tabs.Screen
				name="list"
				options={{
					title: "List",
					tabBarIcon: ({ color, size }) => (
						<IconList size={size} color={color} />
					),
				}}
			/>
			<Tabs.Screen
				name="settings"
				options={{
					title: "Settings",
					tabBarIcon: ({ color, size }) => (
						<IconSettings size={size} color={color} />
					),
				}}
			/>
			<Tabs.Screen
				name="new"
				options={{
					href: null,
				}}
			/>
			<Tabs.Screen
				name="event/[id]"
				options={{
					href: null,
				}}
			/>
			<Tabs.Screen
				name="event/[id]/edit"
				options={{
					href: null,
				}}
			/>
		</Tabs>
	);
}
