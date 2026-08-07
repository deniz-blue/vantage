import { useLocalSearchParams } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { EventResolver, Infer, ResolvedEventContext } from "@vantage/core";
import { Box } from "@/components/base/Box";
import { Container } from "@/components/base/Container";
import { EventDetails } from "@/components/event/details/EventDetails";
import { Text } from "@/components/base/Text";

export default function EventFromIntent() {
	const { at, url, data, type } = useLocalSearchParams<{
		at?: string;
		url?: string;
		data?: string;
		type?: string;
	}>();

	const hasIntent = Boolean(type === "event" || at || url || data);

	const query = useQuery<Vantage.ResolvedEvent>({
		queryKey: ["event-intent", at, url],
		queryFn: async (): Promise<Vantage.ResolvedEvent> => {
			if (at || url) {
				let resolved = Infer.fromString(at ?? url!);
				resolved = await EventResolver.fetchIfNeeded(resolved);
				resolved = await EventResolver.parseIfNeeded(resolved);
				return resolved;
			}

			if (data) {
				const parsed = JSON.parse(data);
				return EventResolver.new({
					source: { type: "unknown" },
					format: { type: "directory.evnt.event" },
					raw: data,
					data: parsed,
				});
			}

			return EventResolver.new();
		},
		enabled: hasIntent,
	});

	if (!hasIntent) {
		return (
			<Box flex={1} align="center" justify="center">
				<Text>No event reference provided.</Text>
			</Box>
		);
	}

	if (query.error) {
		return (
			<Box flex={1} align="center" justify="center">
				<Text>Failed to load event.</Text>
			</Box>
		);
	}

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
