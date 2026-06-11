import { Tabs } from "expo-router";
import { IconHome, IconList, IconSettings } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";

export default function TabLayout() {
	return (
		<Tabs screenOptions={{
			tabBarActiveTintColor: Colors.Primary,
			tabBarInactiveTintColor: Colors.TextDimmed,
			sceneStyle: { backgroundColor: Colors.Background },
			tabBarStyle: {
				backgroundColor: Colors.BackgroundLight,
				borderTopColor: Colors.BackgroundLight,
				borderTopWidth: 0,
				elevation: 0,
			},
			tabBarLabelStyle: {
				color: Colors.Text,
			},
			headerShown: false,
		}}>
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
		</Tabs>
	);
}
