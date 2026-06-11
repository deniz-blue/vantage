import { useLocalSearchParams, Stack } from "expo-router";
import { ActivityIndicator, ScrollView, TouchableOpacity } from "react-native";
import { ResolvedEventContext, useEventQuery } from "@vantage/core";
import { TranslationsUtil } from "@evnt/translations";
import { Box } from "../../../components/base/Box";
import { Text } from "../../../components/base/Text";
import { Colors } from "../../../theme/colors";

export default function EventDetail() {
	const { id } = useLocalSearchParams<{ id: string }>();
	const { data: resolved, isLoading, isError, error } = useEventQuery(id as any);

	if (isLoading) {
		return (
			<Box flex={1} justify="center" align="center" bg={Colors.Background}>
				<ActivityIndicator size="large" color={Colors.Primary} />
			</Box>
		);
	}

	if (isError || !resolved) {
		return (
			<Box flex={1} justify="center" align="center" bg={Colors.Background} p="md">
				<Text style={{ fontSize: 16, color: "#f44336", textAlign: "center" }}>
					{error?.message ?? "Failed to load event"}
				</Text>
			</Box>
		);
	}

	const event = resolved.data;
	const errorInfo = resolved.error;

	const name = event?.name && !TranslationsUtil.isEmpty(event.name)
		? TranslationsUtil.translate(event.name)
		: null;

	const label = event?.label && !TranslationsUtil.isEmpty(event.label)
		? TranslationsUtil.translate(event.label)
		: null;

	const status = event?.status;
	const instance = event?.instances?.[0];
	const previewDate = instance?.start;
	const hasTime = previewDate?.includes("T");

	const formatLabel = resolved.format.type === "directory.evnt.event" ? "Open Evnt"
		: resolved.format.type === "ics" ? "ICS"
		: resolved.format.type;

	const sourceLabel = resolved.source.type === "local" ? "Local"
		: resolved.source.type === "http" ? "HTTP"
		: resolved.source.type;

	return (
		<ResolvedEventContext.Provider value={resolved}>
			<Box flex={1} bg={Colors.Background}>
				<Stack.Screen
					options={{
						title: name ?? "Event",
						headerStyle: { backgroundColor: Colors.BackgroundLight },
						headerTintColor: Colors.Text,
					}}
				/>

				<ScrollView contentContainerStyle={{ padding: 16, gap: 16 }}>
					{/* Header */}
					<Box gap={4}>
						<Text style={{ fontSize: 28, fontWeight: "bold" }}>
							{name ?? "<untitled>"}
						</Text>
						{label && (
							<Text style={{ fontSize: 16, color: Colors.TextDimmed }}>
								{label}
							</Text>
						)}
					</Box>

					{/* Meta badges */}
					<Box direction="row" gap="sm" wrap="wrap">
						{status && status !== "planned" && (
							<MetaBadge label={status} color={statusColor(status)} />
						)}
						<MetaBadge label={formatLabel} color="#00BCD4" />
						<MetaBadge label={sourceLabel} color={sourceLabel === "Local" ? Colors.TextDimmed : "#4CAF50"} />
					</Box>

					{/* Date */}
					{previewDate && (
						<InfoRow label="Date" value={previewDate} />
					)}

					{/* Error banner */}
					{errorInfo && (
						<Box bg="#f4433611" radius={8} p="sm" style={{ borderWidth: 1, borderColor: "#f4433633" }}>
							<Text style={{ color: "#f44336", fontSize: 13 }}>
								{errorInfo.kind}: {errorInfo.message}
							</Text>
						</Box>
					)}

					{/* Raw data section */}
					{resolved.raw && (
						<Box gap="sm">
							<Text style={{ fontSize: 13, color: Colors.TextDimmed, fontWeight: "600" }}>
								Raw Data
							</Text>
							<Box bg={Colors.BackgroundLight} radius={8} p="sm">
								<Text style={{ fontSize: 11, color: Colors.TextDimmed, fontFamily: "monospace" }}>
									{resolved.raw.slice(0, 2000)}
									{resolved.raw.length > 2000 ? "\n..." : ""}
								</Text>
							</Box>
						</Box>
					)}
				</ScrollView>
			</Box>
		</ResolvedEventContext.Provider>
	);
}

// === Helpers ===

const InfoRow = ({ label, value }: { label: string; value: string }) => (
	<Box gap={2}>
		<Text style={{ fontSize: 12, color: Colors.TextDimmed, fontWeight: "600" }}>
			{label}
		</Text>
		<Text style={{ fontSize: 15 }}>
			{value}
		</Text>
	</Box>
);

const MetaBadge = ({ label, color }: { label: string; color: string }) => (
	<Box
		style={{
			backgroundColor: color + "22",
			borderRadius: 6,
			paddingHorizontal: 10,
			paddingVertical: 4,
			borderWidth: 1,
			borderColor: color + "44",
		}}
	>
		<Text style={{ color, fontSize: 12, fontWeight: "600" }}>
			{label}
		</Text>
	</Box>
);

const statusColor = (status: string): string => {
	switch (status) {
		case "cancelled": return "#f44336";
		case "postponed": return "#FF9800";
		case "suspended": return "#9C27B0";
		case "uncertain": return "#FFC107";
		default: return Colors.Primary;
	}
};
