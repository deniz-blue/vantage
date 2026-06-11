import { useState, useCallback } from "react";
import {
	ScrollView,
	View,
	TextInput as RNTextInput,
	TouchableOpacity,
	Alert,
} from "react-native";
import { useRouter } from "expo-router";
import type { PartialDate } from "@evnt/partial-date";
import type { EventData } from "@evnt/schema";
import { EventsManager } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { DateInput, emptyDate, dateToPartialDate } from "../../components/form/DateInput";
import { Colors } from "../../theme/colors";

// === Status options ===

const STATUSES = [
	{ value: "planned", label: "Upcoming" },
	{ value: "uncertain", label: "Tentative" },
	{ value: "cancelled", label: "Cancelled" },
] as const;

type Status = (typeof STATUSES)[number]["value"];

// === Page ===

export default function NewEventPage() {
	const router = useRouter();
	const [name, setName] = useState("");
	const [label, setLabel] = useState("");
	const [status, setStatus] = useState<Status>("planned");
	const [date, setDate] = useState(emptyDate());
	const [saving, setSaving] = useState(false);

	const canSave = name.trim().length > 0 && date.year.length > 0;

	const handleSave = useCallback(async () => {
		if (!canSave || saving) return;

		setSaving(true);
		try {
			const partialDate = dateToPartialDate(date);

			const eventData: EventData = {
				v: "0.1",
				name: { en: name.trim() },
				...(label.trim() ? { label: { en: label.trim() } } : {}),
				status: status === "planned" ? undefined : status,
				instances: partialDate
					? [{ venueIds: [] as string[], start: partialDate as PartialDate }]
					: undefined,
			};

			await EventsManager.addEventWithCache({
				format: { type: "directory.evnt.event" },
				source: { type: "local" },
				raw: JSON.stringify(eventData),
				parsed: eventData,
				error: null,
			});

			router.push("/(tabs)/list");
		} catch (err) {
			console.error("Failed to create event:", err);
			Alert.alert("Error", "Failed to create event. Check console for details.");
		} finally {
			setSaving(false);
		}
	}, [name, label, status, date, canSave, saving, router]);

	return (
		<ScrollView
			style={{ flex: 1, backgroundColor: Colors.Background }}
			contentContainerStyle={{ padding: 16, paddingBottom: 40 }}
			keyboardShouldPersistTaps="handled"
		>
			{/* Header */}
			<Text style={{ fontSize: 28, fontWeight: "bold", marginBottom: 24 }}>
				New Event
			</Text>

			{/* Name */}
			<Box style={{ marginBottom: 16 }}>
				<Text style={{ fontSize: 13, color: Colors.TextDimmed, marginBottom: 6 }}>
					Event Name *
				</Text>
				<RNTextInput
					value={name}
					onChangeText={setName}
					placeholder="e.g. Summer Meetup"
					placeholderTextColor={Colors.TextDimmed}
					autoFocus
					style={{
						backgroundColor: Colors.BackgroundLight,
						color: Colors.Text,
						borderRadius: 8,
						paddingHorizontal: 14,
						paddingVertical: 12,
						fontSize: 17,
						borderWidth: 1,
						borderColor: "transparent",
					}}
				/>
			</Box>

			{/* Label */}
			<Box style={{ marginBottom: 16 }}>
				<Text style={{ fontSize: 13, color: Colors.TextDimmed, marginBottom: 6 }}>
					Label
				</Text>
				<RNTextInput
					value={label}
					onChangeText={setLabel}
					placeholder="e.g. Annual community gathering"
					placeholderTextColor={Colors.TextDimmed}
					style={{
						backgroundColor: Colors.BackgroundLight,
						color: Colors.Text,
						borderRadius: 8,
						paddingHorizontal: 14,
						paddingVertical: 12,
						fontSize: 17,
						borderWidth: 1,
						borderColor: "transparent",
					}}
				/>
			</Box>

			{/* Status */}
			<Box style={{ marginBottom: 24 }}>
				<Text style={{ fontSize: 13, color: Colors.TextDimmed, marginBottom: 8 }}>
					Status
				</Text>
				<View style={{ flexDirection: "row", gap: 8 }}>
					{STATUSES.map((s) => (
						<TouchableOpacity
							key={s.value}
							onPress={() => setStatus(s.value)}
							style={{ flex: 1 }}
						>
							<Box
								style={{
									paddingVertical: 10,
									paddingHorizontal: 12,
									borderRadius: 8,
									alignItems: "center",
									backgroundColor: status === s.value ? Colors.Primary : Colors.BackgroundLight,
									borderWidth: 1,
									borderColor: status === s.value ? Colors.Primary : "transparent",
								}}
							>
								<Text
									style={{
										fontSize: 13,
										fontWeight: "600",
										color: status === s.value ? "#fff" : Colors.TextDimmed,
									}}
								>
									{s.label}
								</Text>
							</Box>
						</TouchableOpacity>
					))}
				</View>
			</Box>

			{/* Date & Time */}
			<Box style={{ marginBottom: 32 }}>
				<Text style={{ fontSize: 13, color: Colors.TextDimmed, marginBottom: 8 }}>
					Date & Time
				</Text>
				<DateInput value={date} onChange={setDate} />
			</Box>

			{/* Create button */}
			<TouchableOpacity
				onPress={handleSave}
				disabled={!canSave || saving}
				style={{
					opacity: canSave && !saving ? 1 : 0.4,
				}}
			>
				<Box
					style={{
						backgroundColor: Colors.Primary,
						borderRadius: 12,
						paddingVertical: 14,
						alignItems: "center",
					}}
				>
					<Text
						style={{
							color: "#fff",
							fontSize: 17,
							fontWeight: "bold",
						}}
					>
						{saving ? "Creating..." : "✦ Create Event"}
					</Text>
				</Box>
			</TouchableOpacity>
		</ScrollView>
	);
}
