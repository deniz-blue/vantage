import { useLocalSearchParams, Stack } from "expo-router";
import { useEventQuery, ResolvedEventContext } from "@vantage/core";
import { Box } from "@/components/base/Box";
import { EventDetails } from "@/components/event/details/EventDetails";
import { Colors } from "@/theme/colors";
import { useTranslator } from "../../../../hooks/useTranslator";

export default function EventDetail() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const { data } = useEventQuery(id as any);
	const translate = useTranslator();

	const title = translate(data?.data?.name);

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

			<ResolvedEventContext.Provider value={data ?? null}>
				<EventDetails />
			</ResolvedEventContext.Provider>
		</Box>
	);
};
