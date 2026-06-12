import { useState } from "react";
import { FlatList, TouchableOpacity } from "react-native";
import { IconWorld, IconArrowUp, IconArrowLeft } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import {
	Combobox,
	ComboboxTrigger,
	ComboboxIcon,
	ComboboxBadge,
	ComboboxSheet,
	ComboboxList,
} from "../base/Combobox";
import { Colors } from "../../theme/colors";

// === Offset computation — module-level cache ===

const offsetCache = new Map<string, { numeric: number; display: string }>();

const computeOffset = (tz: string): { numeric: number; display: string } => {
	const cached = offsetCache.get(tz);
	if (cached) return cached;

	try {
		const now = Date.now();
		const formatter = new Intl.DateTimeFormat("en", {
			timeZone: tz,
			timeZoneName: "shortOffset",
		});
		const parts = formatter.formatToParts(now);
		const raw = parts.find((p) => p.type === "timeZoneName")?.value || "";

		const sign = raw.startsWith("GMT-") ? -1 : 1;
		const cleaned = raw.replace(/^GMT[+-]/, "");
		const [h, m] = cleaned.split(":");
		const hours = Number(h) || 0;
		const minutes = Number(m) || 0;
		const numeric = sign * (hours * 60 + minutes);
		const display = raw.replace(/^GMT/, "UTC");
		const result = { numeric, display };
		offsetCache.set(tz, result);
		return result;
	} catch {
		const result = { numeric: 9999, display: "" };
		offsetCache.set(tz, result);
		return result;
	}
};

const formatOffset = (tz: string): string => computeOffset(tz).display;

// === Sorted & grouped timezone data (module-level) ===

const allTimezones: string[] = Intl.supportedValuesOf("timeZone")
	.map((tz) => ({ tz, offset: computeOffset(tz).numeric }))
	.sort((a, b) => a.offset - b.offset || a.tz.localeCompare(b.tz))
	.map(({ tz }) => tz);

interface Region {
	name: string;
	count: number;
}

const allRegions: Region[] = Object.entries(
	allTimezones.reduce<Record<string, number>>((acc, tz) => {
		const region = tz.includes("/") ? tz.split("/")[0]! : "Other";
		acc[region] = (acc[region] ?? 0) + 1;
		return acc;
	}, {}),
)
	.sort(([a], [b]) => a.localeCompare(b))
	.map(([name, count]) => ({ name, count }));

const timezonesInRegion = (region: string): string[] =>
	region === "Other"
		? allTimezones.filter((tz) => !tz.includes("/"))
		: allTimezones.filter((tz) => tz.startsWith(region + "/"));

const detectedTz = Intl.DateTimeFormat().resolvedOptions().timeZone;

// === Two-step picker (renders inside ComboboxSheet) ===

const TimezonePicker = () => {
	const [region, setRegion] = useState<string | null>(null);

	if (region) {
		return (
			<>
				<Box>
					<TouchableOpacity
						onPress={() => setRegion(null)}
						activeOpacity={0.7}
						style={{
							paddingHorizontal: 10,
							paddingVertical: 8,
							flexDirection: "row",
							alignItems: "center",
							gap: 6,
						}}
					>
						<IconArrowLeft size={18} color={Colors.Primary} />
						<Text fz={14} c="Primary" fw="500">
							{region}
						</Text>
					</TouchableOpacity>
				</Box>

				<ComboboxList
					data={timezonesInRegion(region)}
					renderItem={(tz, selected) => {
						const offset = formatOffset(tz);
						const sub = tz.replace(/^[^/]+\//, "").replace(/_/g, " ");
						return (
							<>
								<ComboboxBadge selected={selected}>
									<Text fz={12} fw="700" c={selected ? "#fff" : Colors.TextDimmed}>
										{offset || "—"}
									</Text>
								</ComboboxBadge>
								<Box flex={1}>
									<Text fz={14} fw={selected ? "600" : "400"} numberOfLines={1}>
										{sub}
									</Text>
								</Box>
							</>
						);
					}}
				/>
			</>
		);
	}

	return (
		<FlatList
			data={allRegions}
			keyExtractor={(item) => item.name}
			style={{ flex: 1 }}
			keyboardShouldPersistTaps="handled"
			contentContainerStyle={{ paddingVertical: 4 }}
			renderItem={({ item }) => (
				<TouchableOpacity
					onPress={() => setRegion(item.name)}
					activeOpacity={0.7}
					style={{
						paddingHorizontal: 16,
						paddingVertical: 14,
						flexDirection: "row",
						alignItems: "center",
						borderRadius: 10,
						marginHorizontal: 8,
						marginVertical: 2,
					}}
				>
					<ComboboxBadge selected={false}>
						<Text fz={12} fw="700" c={Colors.TextDimmed}>
							{item.count}
						</Text>
					</ComboboxBadge>
					<Box flex={1}>
						<Text fz={15} fw="500">
							{item.name}
						</Text>
					</Box>
				</TouchableOpacity>
			)}
		/>
	);
};

// === Root ===

export interface TimezoneSelectProps {
	value: string;
	onChange: (value: string) => void;
}

export const TimezoneSelect = ({ value, onChange }: TimezoneSelectProps) => {
	const currentOffset = formatOffset(value);

	return (
		<Box>
			<Text fz={13} c={Colors.TextDimmed} fw="600" mb={8} ml={2}>
				Timezone
			</Text>
			<Combobox value={value} onChange={onChange}>
				<ComboboxTrigger>
					<ComboboxIcon>
						<IconWorld size={20} color={Colors.Primary} />
					</ComboboxIcon>
					<Box flex={1}>
						<Text fz={15} fw="600">
							{value}
						</Text>
						<Text fz={12} c={Colors.TextDimmed} mt={1}>
							{currentOffset}
						</Text>
					</Box>
				</ComboboxTrigger>
				<ComboboxSheet height={0.85}>
					<TimezonePicker />
				</ComboboxSheet>
			</Combobox>

			{detectedTz !== value && (
				<TouchableOpacity
					onPress={() => onChange(detectedTz)}
					activeOpacity={0.7}
					style={{
						marginTop: 8,
						flexDirection: "row",
						alignItems: "center",
						backgroundColor: Colors.PrimaryLight + "22",
						paddingVertical: 8,
						paddingHorizontal: 12,
						borderRadius: 8,
						gap: 6,
					}}
				>
					<IconArrowUp size={14} color={Colors.Primary} />
					<Text fz={13} c="Primary" fw="500">
						Use detected: {detectedTz} ({formatOffset(detectedTz)})
					</Text>
				</TouchableOpacity>
			)}
		</Box>
	);
};
