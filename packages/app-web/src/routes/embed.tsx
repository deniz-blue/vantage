import { createFileRoute } from "@tanstack/react-router"
import z from "zod";
import { zodValidator } from "@tanstack/zod-adapter";
import { Center, Stack, Text } from "@mantine/core";
import { EventCard } from "../components/content/event/card/EventCard";
import { useQuery } from "@tanstack/react-query";
import { RemoteUriSchema } from "../lib/intent";
import { inferSourceFormat, parseEventFormat } from "@vantage/core";
import { ResolvedEventContext } from "@vantage/core";
import { eventQueryFnNoId } from "@vantage/core";

export const sourceOrDataSchema = z.object({
	source: RemoteUriSchema.optional(),
	data: z.unknown().optional(),
});

export const fetchResolvedEventFromQuery = async (search: z.infer<typeof sourceOrDataSchema>): Promise<Vantage.ResolvedEvent> => {
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

	const { source, format } = await inferSourceFormat(search.source);
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
					embedSource={search.source}
					variant="card"
					embed
				/>
			</ResolvedEventContext>
			<style children="html, body, #root { height: 100%; margin: 0; background: transparent !important; }" />
		</Stack>
	)
}
