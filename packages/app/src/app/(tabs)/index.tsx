import { Box } from "../../components/base/Box";
import { useHomeStore } from "../../stores/useHomeStore";
import { HomeWidget } from "../../components/home/HomeWidget";
import { Container } from "../../components/base/Container";
import { SafeAreaView } from "react-native-safe-area-context";
import { ScrollView } from "react-native-gesture-handler";
import { Text } from "../../components/base/Text";
import { IconHome } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { IconSize } from "../../theme/sizing";

export default function IndexPage() {
	const widgets = useHomeStore((s) => s.widgets);

	return (
		<Box component={ScrollView} flex={1}>
			<Box component={SafeAreaView} flex={1}>
				<Container size="lg" flex={1} px={0} gap="md">
					<Box px="md" pt="md" gap="sm" justify="center" direction="row">
						<IconHome size={IconSize.md} color={Colors.Text} />
						<Text>Home</Text>
					</Box>

					{widgets.map((widget, index) => (
						<HomeWidget key={index} widget={widget} />
					))}
				</Container>
			</Box>
		</Box>
	);
}
