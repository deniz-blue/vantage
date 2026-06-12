import { useState, useCallback } from "react";
import { ScrollView, View, Alert } from "react-native";
import { useRouter } from "expo-router";
import type { PartialDate, OpenEvnt } from "@evnt/types";
import { EventsManager } from "@vantage/core";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Button } from "../../components/base/Button";
import { TextInput } from "../../components/base/TextInput";
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

			const eventData: OpenEvnt = {
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
		<Box flex={1} px="md">
			<ScrollView
				style={{ flex: 1, backgroundColor: Colors.Background }}
				contentContainerStyle={{ paddingBottom: 40 }}
				keyboardShouldPersistTaps="handled"
			>
				<Box pt="md">
					<Text fz={24} fw="bold" mb="lg">
						New Event
					</Text>
				</Box>

			<Box gap={16}>
				<TextInput
					label="Event Name *"
					placeholder="e.g. Summer Meetup"
					value={name}
					onChangeText={setName}
					autoFocus
				/>

				<TextInput
					label="Label"
					placeholder="e.g. Annual community gathering"
					value={label}
					onChangeText={setLabel}
				/>

				<Box gap={8}>
					<Text fz={13} c={Colors.TextDimmed}>
						Status
					</Text>
					<View style={{ flexDirection: "row", gap: 8 }}>
						{STATUSES.map((s) => (
							<Button
								key={s.value}
								variant={status === s.value ? "filled" : "subtle"}
								color={status === s.value ? Colors.Primary : undefined}
								size="sm"
								fullWidth
								onPress={() => setStatus(s.value)}
							>
								{s.label}
							</Button>
						))}
					</View>
				</Box>

				<Box gap={8}>
					<Text fz={13} c={Colors.TextDimmed}>
						Date & Time
					</Text>
					<DateInput value={date} onChange={setDate} />
				</Box>

				<Button
					onPress={handleSave}
					disabled={!canSave}
					loading={saving}
					fullWidth
					size="lg"
				>
					{saving ? "Creating..." : "✧ Create Event"}
				</Button>
			</Box>
		</ScrollView>
		</Box>
	);
}
