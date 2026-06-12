import { useRouter } from "expo-router";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Fab } from "../../components/base/Fab";

export default function IndexPage() {
	const router = useRouter();

	return (
		<Box flex={1}>
			<Box px="md" pt="md" pb={4}>
				<Text fz={28} fw="bold">
					Vantage
				</Text>
				<Text fz={13} c="TextDimmed">
					Event management
				</Text>
			</Box>

			<Fab onPress={() => router.push("/new")} />
		</Box>
	);
}
