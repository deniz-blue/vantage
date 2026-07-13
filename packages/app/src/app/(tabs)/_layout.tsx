import { useWindowDimensions } from "react-native";
import { Tabs } from "expo-router";
import { IconHome, IconList, IconCalendar, IconSettings } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { PlusFab } from "../../components/app/PlusFab";
import { Box } from "../../components/base/Box";
import { ActionIcon } from "../../components/base/button/ActionIcon";
import { IconSize } from "../../theme/sizing";
import { Container } from "../../components/base/Container";
import { useSafeAreaInsets } from "react-native-safe-area-context";

const WIDE_BREAKPOINT = 640;
const TAB_BAR_MAX_WIDTH = 240;

export default function TabLayout() {
	const insets = useSafeAreaInsets();
	const { width: screenWidth } = useWindowDimensions();
	const isWide = screenWidth >= WIDE_BREAKPOINT;

	return (
		<>
			<PlusFab />

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
				tabBar={(props) => (
					<Box bg={Colors.BackgroundLight} pb={insets.bottom}>
						<Box h={56}>
							<Container direction="row" justify="center" h="100%">
								{(
									[
										{ name: "index", icon: IconHome },
										{ name: "list", icon: IconList },
										{ name: "calendar", icon: IconCalendar },
										{ name: "settings", icon: IconSettings },
									] as const
								).map(({ icon: Icon, name }) => {
									const isActive = props.state.routeNames[props.state.index] === name;
									return (
										<Box key={name} flex={1} direction="row" justify="center">
											<ActionIcon
												onPress={() => props.navigation.navigate(name)}
												size="auto"
												w="100%"
												h="100%"
											>
												<Icon
													size={IconSize.lg}
													color={isActive ? Colors.Primary : Colors.TextDimmed}
												/>
											</ActionIcon>
										</Box>
									);
								})}
							</Container>
						</Box>
					</Box>
				)}
			>
				<Tabs.Screen name="index" />
				<Tabs.Screen name="list" />
				<Tabs.Screen name="settings" />
				<Tabs.Screen name="new" />
				<Tabs.Screen name="calendar" />
			</Tabs>
		</>
	);
}
