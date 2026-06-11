import { Tabs } from "expo-router";
import { Colors } from "../../theme/colors";

export default function TabLayout() {
	return (
		<Tabs screenOptions={{
			tabBarActiveTintColor: "blue",
			sceneStyle: { backgroundColor: Colors.Background },
			tabBarStyle: {
				backgroundColor: Colors.BackgroundLight,
				borderTopColor: Colors.BackgroundLight,
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
				}}
			/>
			<Tabs.Screen
				name="list"
				options={{
					title: "List",
				}}
			/>
			<Tabs.Screen
				name="new"
				options={{
					title: "+",
					tabBarLabel: "New",
				}}
			/>
			<Tabs.Screen
				name="settings"
				options={{
					title: "Settings",
				}}
			/>
		</Tabs>
	);
}
