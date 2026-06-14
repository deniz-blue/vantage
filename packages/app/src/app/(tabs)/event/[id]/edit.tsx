import { useLocalSearchParams, Stack, useRouter } from "expo-router";
import { useEventQuery } from "@vantage/core";
import { Box } from "../../../../components/base/Box";
import { Text } from "../../../../components/base/Text";
import { Button } from "../../../../components/base/Button";
import { Colors } from "../../../../theme/colors";

export default function EditEventPage() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const router = useRouter();
	const { data: resolved, isLoading, isError } = useEventQuery(id as any);

	const isLocal = resolved?.source?.type === "local";

	// -- Guard: non-local
	if (!isLoading && !isError && resolved && !isLocal) {
		return (
			<Box flex={1} px="md" pt="md" align="center" justify="center">
				<Box mb="md">
					<Text ta="center">
						This event is from a remote source and can't be edited here yet.
					</Text>
				</Box>
				<Button onPress={() => router.back()}>Go Back</Button>
			</Box>
		);
	}

	// -- Guard: error
	if (!isLoading && isError) {
		return (
			<Box flex={1} px="md" pt="md" align="center" justify="center">
				<Box mb="md">
					<Text ta="center">Could not load event.</Text>
				</Box>
				<Button onPress={() => router.back()}>Go Back</Button>
			</Box>
		);
	}

	const title = resolved?.data?.name?.en ?? "Edit Event";

	// -- Guard: loading
	if (isLoading || !resolved) {
		return (
			<Box flex={1} px="md" pt="md">
				<Text>Loading event…</Text>
			</Box>
		);
	}

	return (
		<Box flex={1} bg={Colors.Background}>
			<Stack.Screen
				options={{
					title,
					headerStyle: { backgroundColor: Colors.BackgroundLight } as any,
					headerTintColor: Colors.Text,
					headerShadowVisible: false,
				}}
			/>
		</Box>
	);
}
