import { createFileRoute } from "@tanstack/react-router"
import z from "zod";
import { zodValidator } from "@tanstack/zod-adapter";
import { Center, Stack, Text } from "@mantine/core";
import { EventCard } from "../components/content/event/card/EventCard";
import { useQuery } from "@tanstack/react-query";
import { RemoteUriSchema, remoteUriToSourceFormat } from "../lib/intent";
import { parseEventFormat } from "@vantage/core";
import { ResolvedEvent, ResolvedEventContext } from "../db/resolved-event";
import { eventQueryFnNoId } from "../db/useEventQuery";

export const sourceOrDataSchema = z.object({
	source: RemoteUriSchema.optional(),
	data: z.unknown().optional(),
});

export const fetchResolvedEventFromQuery = async (search: z.infer<typeof sourceOrDataSchema>): Promise<ResolvedEvent> => {
	if (search.data) {
		const raw = JSON.stringify(search.data);
		const format: Vantage.EventFormat = { type: "directory.evnt.event" };
		const { parsed, error } = parseEventFormat(raw, format);
		return {
			id: null,
			data: parsed,
			raw,
			error,
			revision: {},
			source: { type: "unknown" },
			format,
		};
	}

	if (!search.source) throw new Error("Either source or data must be provided");

	const { source, format } = remoteUriToSourceFormat(search.source);
	return await eventQueryFnNoId(source, format);
};

export const Route = createFileRoute("/embed")({
	component: EmbedPage,
	validateSearch: zodValidator(sourceOrDataSchema),
});

export function EmbedPage() {
	const search = Route.useSearch();

	const query = useQuery({
		queryKey: ["embed-event-data", JSON.stringify(search.data)],
		queryFn: () => fetchResolvedEventFromQuery(search),
	});

	if (query.error) return (
		<Center w="100%" h="100%" style={{ height: "100%" }}>
			<Text c="yellow">
				{`Failed to load event data: ${query.error instanceof Error ? query.error.message : String(query.error)}`}
			</Text>
		</Center>
	)

	return (
		<Stack align="center" justify="center" style={{ height: "100%" }}>
			<ResolvedEventContext value={query.data ?? null}>
				<EventCard
					loading={query?.isLoading ?? false}
					variant="card"
					embed
				/>
			</ResolvedEventContext>
			<style children="html, body, #root { height: 100%; margin: 0; }" />
		</Stack>
	)
}
