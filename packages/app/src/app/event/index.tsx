import { useLocalSearchParams } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { EventResolver, parseEventFormat, ResolvedEventContext } from "@vantage/core";
import { Box } from "@/components/base/Box";
import { Container } from "@/components/base/Container";
import { EventDetails } from "@/components/event/details/EventDetails";
import { Text } from "@/components/base/Text";

const detectFormat = (raw: string, contentType: string, url: string): Vantage.EventFormat => {
	if (contentType.includes("application/json") || url.endsWith(".json")) {
		try {
			const data = JSON.parse(raw);
			if (typeof data?.$type === "string") return { type: data.$type };
		} catch { /* fall through */ }
		return { type: "directory.evnt.event" };
	}

	if (contentType.includes("text/calendar") || url.endsWith(".ics")) {
		return { type: "ics" };
	}

	return { type: "unknown" };
};

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
			if (at) {
				let resolved = EventResolver.new({
					source: { type: "at", uri: at as any },
					format: { type: "unknown" },
				});
				resolved = await EventResolver.fetch(resolved);

				if (resolved.raw) {
					const record = JSON.parse(resolved.raw);
					if (typeof record.$type === "string") {
						resolved.format = { type: record.$type };
					}
				}

				return await EventResolver.parse(resolved);
			}

			if (url) {
				const res = await fetch(url);
				if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);

				const raw = await res.text();
				const contentType = res.headers.get("content-type") ?? "";
				const format = detectFormat(raw, contentType, url);
				const { parsed, error } = parseEventFormat(raw, format, { type: "http", url });

				return EventResolver.new({
					source: { type: "http", url },
					format,
					raw,
					data: parsed,
					error,
					revision: {
						etag: res.headers.get("ETag") ?? undefined,
						lastModifiedHeader: res.headers.get("Last-Modified") ?? undefined,
					},
				});
			}

			if (data) {
				const parsed = JSON.parse(data);
				const raw = JSON.stringify(parsed);
				const format: Vantage.EventFormat = { type: "directory.evnt.event" };
				const { parsed: eventData, error } = parseEventFormat(raw, format);

				return EventResolver.new({
					source: { type: "unknown" },
					format,
					raw,
					data: eventData,
					error,
				});
			}

			throw new Error("No event reference provided");
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
					<EventDetails
						loading={query.isFetching}
						onRefresh={query.refetch}
					/>
				</Container>
			</Box>
		</ResolvedEventContext>
	);
}
