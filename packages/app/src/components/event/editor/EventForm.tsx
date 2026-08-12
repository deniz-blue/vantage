import { OpenEvnt } from "@evnt/types";
import { Editor } from "./editor";
import { EventFormContext } from "./event-form-context";
import { useState } from "react";
import { SceneMap, TabBar, TabView, type Route } from "react-native-tab-view";
import { TabMain } from "./tabs/TabMain";
import { TabTimePlace } from "./tabs/TabTimePlace";
import { TabComponents } from "./tabs/TabComponents";
import { useWindowDimensions } from "react-native";
import { Colors } from "../../../theme/colors";
import { Box } from "../../base/Box";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Container } from "../../base/Container";
import { Font } from "../../../theme/sizing";

const renderScene = SceneMap({
	main: TabMain,
	timePlace: TabTimePlace,
	components: TabComponents,
});

const routes: Route[] = [
	{ key: "main", title: "Event" },
	{ key: "timePlace", title: "Time & Place" },
	{ key: "components", title: "Details" },
];

export const EventFormPage = ({
	editor,
	header,
	action,
}: {
	editor: Editor<OpenEvnt>;
	header: React.ReactNode;
	action: React.ReactNode;
}) => {
	const insets = useSafeAreaInsets();
	const layout = useWindowDimensions();
	const [index, setIndex] = useState(0);

	console.log(JSON.stringify(editor.value, null, 2));

	return (
		<EventFormContext value={{ editor }}>
			<Container size="sm" px={0} flex={1}>
				<TabView
					navigationState={{ index, routes }}
					renderScene={renderScene}
					onIndexChange={setIndex}
					initialLayout={{ width: layout.width }}
					style={{ paddingTop: insets.top }}
					commonOptions={{
						labelStyle: {
							color: Colors.Text,
							fontFamily: Font.Default,
						},
					}}
					renderTabBar={(props) => (
						<Box px="md" pt="md">
							{header}
							<TabBar
								{...props}
								style={{ backgroundColor: "transparent" }}
								indicatorStyle={{ backgroundColor: Colors.Text }}
								inactiveColor={Colors.TextDimmed}
							/>
						</Box>
					)}
				/>

				<Box pos="absolute" style={{ bottom: insets.bottom }} w="100%">
					<Container size="sm" flex={1} pb="md">
						{action}
					</Container>
				</Box>
			</Container>
		</EventFormContext>
	);
};
