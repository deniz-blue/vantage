import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { useHomeStore } from "../../stores/useHomeStore";
import { HomeWidget } from "../../components/home/HomeWidget";
import { Container } from "../../components/base/Container";
import { SafeAreaView } from "react-native-safe-area-context";

export default function IndexPage() {
	const widgets = useHomeStore((s) => s.widgets);

	return (
		<Box component={SafeAreaView} flex={1}>
			<Container size="lg" flex={1} px={0} gap="md">
				<Box px="md" pt="md" pb={4}>
					<Text fz={24} fw="bold">
						Vantage
					</Text>
					<Text fz={13} c="TextDimmed">
						An open-source event calendar for everyone
					</Text>
				</Box>

				{widgets.map((widget, index) => (
					<HomeWidget key={index} widget={widget} />
				))}
			</Container>
		</Box>
	);
}
