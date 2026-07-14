import { useLocalSearchParams } from "expo-router";
import { useEventQuery, ResolvedEventContext } from "@vantage/core";
import { Box } from "@/components/base/Box";
import { Container } from "@/components/base/Container";
import { EventDetails } from "@/components/event/details/EventDetails";

export default function EventDetail() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const query = useEventQuery(id as any);

	return (
		<ResolvedEventContext value={query.data ?? null}>
			<Box flex={1} bg="Dark8">
				<Container size="lg" bg="Dark7" flex={1} px={0}>
					<EventDetails loading={query.isFetching} onRefresh={query.refetch} />
				</Container>
			</Box>
		</ResolvedEventContext>
	);
}
