import { createFileRoute } from "@tanstack/react-router"
import { eventQueryFn, eventQueryFnNoId, eventQueryKey, inferSourceFormat } from "@vantage/core";
import { Container, Space, Stack } from "@mantine/core";
import { EventDetailsContent } from "../../components/content/event/details/EventDetailsContent";
import { useProvideEventActions } from "../../hooks/actions/useProvideEventActions";
import { ResolvedEventContext } from "@vantage/core";
import { useQuery } from "@tanstack/react-query";
import z from "zod";

const SearchParamsSchema = z.object({
	id: (z.uuid() as z.ZodType<Vantage.EventId>).optional(),
	source: z.string().optional(),
});

export const Route = createFileRoute("/_layout/event")({
	component: EventPage,
	validateSearch: SearchParamsSchema,
	staticData: {
		spaceless: true,
	},
});

function EventPage() {
	const { id, source } = Route.useSearch();
	const query = useQuery({
		queryKey: id ? eventQueryKey(id) : ["source", source] as const,
		queryFn: async () => {
			if (id) return await eventQueryFn(id);
			if (source) {
				const { source: eventSource, format } = await inferSourceFormat(source);
				return await eventQueryFnNoId(eventSource, format);
			}
			throw new Error("Either id or source must be provided");
		},
	});

	useProvideEventActions(query.data);

	return (
		<Stack
			w="100%"
			align="center"
		>
			<Container
				size="md"
				p={0}
				w="100%"
				mih="100dvh"
				style={{
					boxShadow: "0 0 50px rgba(0,0,0,0.2)",
				}}
			>
				<Stack>
					<ResolvedEventContext value={query.data ?? null}>
						<EventDetailsContent />
					</ResolvedEventContext>
					<Space h="20rem" />
				</Stack>
			</Container>
		</Stack>
	)
}
