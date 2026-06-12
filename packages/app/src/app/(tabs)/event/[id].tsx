import { useLocalSearchParams, Stack } from "expo-router";
import { ScrollView } from "react-native";
import { ResolvedEventContext, useResolvedEvent, useEventQuery } from "@vantage/core";
import { Colors } from "../../../theme/colors";
import { Box } from "../../../components/base/Box";
import { Text } from "../../../components/base/Text";
import { Badge } from "../../../components/base/Badge";
import { Card } from "../../../components/base/Card";
import { EmptyState } from "../../../components/base/EmptyState";

export default function EventDetail() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const { data: resolved, isLoading, isError, error } = useEventQuery(id as any);

	const title = resolved?.data?.name
		? resolved.data.name.en ?? "Event"
		: "Event";

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

			{isLoading ? (
				<EmptyState message="Loading event…" />
			) : isError || !resolved ? (
				<EmptyState
					loading={false}
					message={error?.message ?? "Failed to load event"}
				/>
			) : (
				<ResolvedEventContext.Provider value={resolved}>
					<ScrollView contentContainerStyle={{ paddingBottom: 32 }}>
						<Box px="md" pt="md" gap={16}>
							<EventHeader />
							<EventMetaBar />
							<EventDate />
							<EventError />
							<EventRawData />
						</Box>
					</ScrollView>
				</ResolvedEventContext.Provider>
			)}
		</Box>
	);
}

// === Event sections ===

const EventHeader = () => {
	const { data: event } = useResolvedEvent();
	if (!event) return null;

	const name = event.name?.en ?? "<untitled>";
	const label = event.label?.en;

	return (
		<Box gap={4}>
			<Text style={{ fontSize: 28, fontWeight: "bold" }}>
				{name}
			</Text>
			{label && (
				<Text style={{ fontSize: 16, color: Colors.TextDimmed }}>
					{label}
				</Text>
			)}
		</Box>
	);
};

const EventMetaBar = () => {
	const { data: event, format, source } = useResolvedEvent();
	if (!event) return null;

	const status = event.status;
	const formatLabel = format.type === "directory.evnt.event" ? "Open Evnt"
		: format.type === "ics" ? "ICS"
		: format.type;
	const sourceLabel = source.type === "local" ? "Local"
		: source.type === "http" ? "HTTP"
		: source.type;

	return (
		<Box direction="row" gap="sm" wrap="wrap">
			{status && status !== "planned" && (
				<Badge size="sm" color={statusColor(status)}>
					{status}
				</Badge>
			)}
			<Badge size="sm" color="Cyan">{formatLabel}</Badge>
			<Badge size="sm" color={sourceLabel === "Local" ? "Grey" : "Green"}>
				{sourceLabel}
			</Badge>
		</Box>
	);
};

const EventDate = () => {
	const { data: event } = useResolvedEvent();
	const instance = event?.instances?.[0];
	const start = instance?.start;

	if (!start) return null;

	return (
		<Box gap={2}>
			<Text style={{ fontSize: 12, color: Colors.TextDimmed, fontWeight: "600" }}>
				Date
			</Text>
			<Text style={{ fontSize: 15 }}>
				{start}
			</Text>
		</Box>
	);
};

const EventError = () => {
	const { error } = useResolvedEvent();
	if (!error) return null;

	return (
		<Card p="sm" bg={Colors.Red + "11"} style={{ borderWidth: 1, borderColor: Colors.Red + "33" }}>
			<Text style={{ color: Colors.Red, fontSize: 13 }}>
				{error.kind}: {error.message}
			</Text>
		</Card>
	);
};

const EventRawData = () => {
	const { raw } = useResolvedEvent();
	if (!raw) return null;

	return (
		<Box gap="sm">
			<Text style={{ fontSize: 13, color: Colors.TextDimmed, fontWeight: "600" }}>
				Raw Data
			</Text>
			<Card bg={Colors.BackgroundLight} p="sm">
				<Text style={{ fontSize: 11, color: Colors.TextDimmed, fontFamily: "monospace" }}>
					{raw.slice(0, 2000)}
					{raw.length > 2000 ? "\n..." : ""}
				</Text>
			</Card>
		</Box>
	);
};

// === Colors ===

const statusColor = (status: string): string => {
	switch (status) {
		case "cancelled": return "Red";
		case "postponed": return "Orange";
		case "suspended": return "Purple";
		case "uncertain": return "Yellow";
		default: return Colors.Primary;
	}
};
