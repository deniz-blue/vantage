import { useRouter } from "expo-router";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Fab } from "../../components/base/Fab";
import { useHomeStore } from "../../stores/useHomeStore";
import { HomeWidget } from "../../components/home/HomeWidget";
import { Container } from "../../components/base/Container";

export default function IndexPage() {
	const router = useRouter();
	const widgets = useHomeStore((s) => s.widgets);

	return (
		<Container size="lg" flex={1} px={0}>
			<Box px="md" pt="md" pb={4}>
				<Text fz={24} fw="bold">
					Vantage
				</Text>
				<Text fz={13} c="TextDimmed">
					An open-source event calendar for everyone
				</Text>
			</Box>

			<Fab onPress={() => router.push("/new")} />

			{widgets.map((widget, index) => (
				<HomeWidget key={index} widget={widget} />
			))}
		</Container>
	);
}
