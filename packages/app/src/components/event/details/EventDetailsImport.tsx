import { EventsManager, useResolvedEvent } from "@vantage/core";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "expo-router";
import { db, schema } from "@vantage/db";
import { and, eq } from "drizzle-orm";
import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { Text } from "../../base/Text";
import { FontSize } from "../../../theme/sizing";
import { AsyncButton } from "../../base/button/AsyncButton";

export const EventDetailsImport = () => {
	const { id, source, format } = useResolvedEvent();
	const router = useRouter();

	const unknown = source.type == "unknown" || format.type == "unknown";

	const existing = useQuery({
		queryKey: ["exists", { source, format }],
		enabled: !id && !unknown,
		queryFn: async () => {
			return await db
				.select()
				.from(schema.eventMeta)
				.where(and(eq(schema.eventMeta.source, source), eq(schema.eventMeta.format, format)));
		},
	});

	const showRedirect = !id && !unknown && (existing.data?.length ?? 0) > 0;
	const showImport = !id && !unknown && (existing.data?.length ?? 0) === 0;

	if (!showRedirect && !showImport) return null;

	return (
		<Box gap={0}>
			{showRedirect && (
				<Button
					onPress={() => {
						const existingId = existing.data?.[0].id;
						if (existingId) router.replace(`/event/${existingId}`);
					}}
					color="blue"
					h="auto"
					mb="xs"
				>
					<Box gap={4} my="xs" align="center" flex={1}>
						<Text fw="bold">View in My Events</Text>
						<Text fz={FontSize.xs}>This event is already in your list</Text>
					</Box>
				</Button>
			)}

			{showImport && (
				<AsyncButton
					fn={async () => {
						const id = await EventsManager.addEvent({ source, format });
						router.replace(`/event/${id}`);
					}}
				>
					{({ loading, onPress }) => (
						<Button onPress={onPress} loading={loading} color="green" h="auto" mb="xs">
							<Box gap={4} my={4} align="center" flex={1}>
								<Text fw="bold">Add to My Events</Text>
								<Text fz={FontSize.xs}>offline accessible and shown in your list</Text>
							</Box>
						</Button>
					)}
				</AsyncButton>
			)}
		</Box>
	);
};
