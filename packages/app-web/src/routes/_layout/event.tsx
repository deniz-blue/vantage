import { createFileRoute } from "@tanstack/react-router"
import { eventQueryFn, eventQueryFnInferFromStr, eventQueryKey } from "@vantage/core";
import { Container, Space, Stack } from "@mantine/core";
import { EventDetailsContent } from "../../components/content/event/details/EventDetailsContent";
import { useProvideEventActions } from "../../hooks/actions/useProvideEventActions";
import { ResolvedEventContext } from "@vantage/core";
import { useQuery } from "@tanstack/react-query";
import z from "zod";
import classes from "./event.module.css";

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
			if (source) return await eventQueryFnInferFromStr(source);
			throw new Error("Either id or source must be provided");
		},
	});

	useProvideEventActions(query.data);

	return (
		<Stack
			w="100%"
			align="center"
			className={classes.main}
		>
			<Container
				className={classes.container}
				size="md"
			>
				<Stack>
					<ResolvedEventContext value={query.data ?? null}>
						<EventDetailsContent />
					</ResolvedEventContext>
					<Space h="40vh" />
				</Stack>
			</Container>
		</Stack>
	)
}
