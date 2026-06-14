import { useState } from "react";
import { FlatList, TouchableOpacity } from "react-native";
import { IconWorld, IconChevronDown, IconArrowUp, IconArrowLeft } from "@tabler/icons-react-native";
import { Box } from "./Box";
import { Text } from "./Text";
import { Button } from "./Button";
import { InputWrapper, type InputWrapperProps } from "./InputWrapper";
import { Sheet } from "./Sheet";
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

const allTimezones: string[] = (() => {
	try {
		return Intl.supportedValuesOf("timeZone")
			.map((tz: string) => ({ tz, offset: computeOffset(tz).numeric }))
			.sort((a, b) => a.offset - b.offset || a.tz.localeCompare(b.tz))
			.map(({ tz }) => tz);
	} catch {
		return [
			"UTC", "America/New_York", "America/Chicago", "America/Denver",
			"America/Los_Angeles", "America/Vancouver", "Europe/London",
			"Europe/Paris", "Europe/Berlin", "Europe/Vilnius", "Europe/Helsinki",
			"Asia/Tokyo", "Asia/Shanghai", "Asia/Kolkata", "Australia/Sydney",
			"Pacific/Auckland",
		];
	}
})();

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

const detectedTz = (() => {
	try {
		return Intl.DateTimeFormat().resolvedOptions().timeZone;
	} catch {
		return "UTC";
	}
})();

// === Props ===

export interface TimezoneSelectProps
	extends Pick<InputWrapperProps, "label" | "description" | "error" | "required"> {
	value: string;
	onChange: (value: string) => void;
	variant?: "settings" | "form";
}

// === Component ===

export const TimezoneSelect = ({
	label,
	description,
	error,
	required,
	value,
	onChange,
	variant = "settings",
}: TimezoneSelectProps) => {
	const [open, setOpen] = useState(false);
	const [region, setRegion] = useState<string | null>(null);

	const currentOffset = formatOffset(value);

	const handleSelect = (tz: string) => {
		onChange(tz);
		setOpen(false);
	};

	// Reset region when sheet opens
	const handleOpen = () => {
		setRegion(null);
		setOpen(true);
	};

	// === Trigger ===

	const triggerBody =
		variant === "form" ? (
			<Button
				variant="subtle"
				size="sm"
				onPress={handleOpen}
				rightSection={<IconChevronDown size={12} color={Colors.Primary} />}
			>
				{value}
			</Button>
		) : (
			<Box>
				<Button
					variant="default"
					onPress={handleOpen}
					leftSection={<IconWorld size={20} color={Colors.Primary} />}
				>
					<Box flex={1}>
						<Text fz={15} fw="600">
							{value}
						</Text>
						<Text fz={12} c={Colors.TextDimmed} mt={1}>
							{currentOffset}
						</Text>
					</Box>
				</Button>
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

	// === Sheet content (two-step region picker) ===

	const sheetContent = region ? (
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

			<FlatList
				data={timezonesInRegion(region)}
				keyExtractor={(item) => item}
				initialNumToRender={30}
				keyboardShouldPersistTaps="handled"
				contentContainerStyle={{ paddingHorizontal: 8, gap: 2, paddingBottom: 32 }}
				style={{ flex: 1 }}
				renderItem={({ item }) => {
					const selected = item === value;
					const offset = formatOffset(item);
					const sub = item.replace(/^[^/]+\//, "").replace(/_/g, " ");
					return (
						<TouchableOpacity
							onPress={() => handleSelect(item)}
							activeOpacity={0.6}
							style={{
								flexDirection: "row",
								alignItems: "center",
								paddingVertical: 10,
								paddingHorizontal: 12,
								borderRadius: 6,
								gap: 10,
								backgroundColor: selected ? Colors.Primary + "22" : "transparent",
							}}
						>
							<Box
								style={{
									minWidth: 52,
									alignItems: "flex-end",
								}}
							>
								<Text fz={12} fw="700" c={selected ? Colors.Primary : Colors.TextDimmed}>
									{offset || "—"}
								</Text>
							</Box>
							<Box flex={1}>
								<Text fz={14} fw={selected ? "600" : "400"} numberOfLines={1}>
									{sub}
								</Text>
							</Box>
						</TouchableOpacity>
					);
				}}
			/>
		</>
	) : (
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
					<Box
						style={{
							minWidth: 52,
							alignItems: "flex-end",
							marginRight: 10,
						}}
					>
						<Text fz={12} fw="700" c={Colors.TextDimmed}>
							{item.count}
						</Text>
					</Box>
					<Box flex={1}>
						<Text fz={15} fw="500">
							{item.name}
						</Text>
					</Box>
				</TouchableOpacity>
			)}
		/>
	);

	return (
		<>
			<InputWrapper
				label={label}
				description={description}
				error={error}
				required={required}
			>
				{triggerBody}
			</InputWrapper>

			<Sheet open={open} onClose={() => setOpen(false)}>
				{sheetContent}
			</Sheet>
		</>
	);
};
