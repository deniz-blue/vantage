import { Tabs } from "expo-router";
import { Colors } from "../../theme/colors";
import { Icon } from "../../components/base/Icon";

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
					tabBarIcon: ({ focused }) => <Icon name="home" c={focused ? "Primary" : "Gray0"} />,
				}}
			/>
			<Tabs.Screen
				name="list"
				options={{
					title: "List",
					tabBarIcon: ({ focused }) => <Icon name="list" c={focused ? "Primary" : "Gray0"} />,
				}}
			/>
			<Tabs.Screen
				name="settings"
				options={{
					title: "Settings",
					tabBarIcon: ({ focused }) => <Icon name="settings" c={focused ? "Primary" : "Gray0"} />,
				}}
			/>
		</Tabs>
	);
}
