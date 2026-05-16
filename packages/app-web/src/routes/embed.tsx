import { createFileRoute } from "@tanstack/react-router"
import z from "zod";
import { zodValidator } from "@tanstack/zod-adapter";
import { Center, Stack, Text } from "@mantine/core";
import { EventCard } from "../components/content/event/card/EventCard";
import { useQuery } from "@tanstack/react-query";
import { RemoteUriSchema } from "../lib/intent";
import { eventQueryFnDataOrSourceStr, inferSourceFormat, parseEventFormat } from "@vantage/core";
import { ResolvedEventContext } from "@vantage/core";
import { eventQueryFnNoId } from "@vantage/core";

export const sourceOrDataSchema = z.object({
	source: RemoteUriSchema.optional(),
	data: z.unknown().optional(),
});

export const Route = createFileRoute("/embed")({
	component: EmbedPage,
	validateSearch: zodValidator(sourceOrDataSchema),
});

export function EmbedPage() {
	const search = Route.useSearch();

	const query = useQuery({
		queryKey: ["embed-event-data", JSON.stringify(search.data)],
		queryFn: () => eventQueryFnDataOrSourceStr(search),
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
