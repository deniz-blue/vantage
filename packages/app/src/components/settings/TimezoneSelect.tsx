import { FlatList, TouchableOpacity, TextInput } from "react-native";
import { useCallback, useMemo, useState } from "react";
import { IconWorld, IconChevronRight, IconCheck, IconSearch, IconArrowUp } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Sheet } from "../base/Sheet";
import { Colors } from "../../theme/colors";

const getOffset = (tz: string): string => {
	try {
		const now = Date.now();
		const formatter = new Intl.DateTimeFormat("en", {
			timeZone: tz,
			timeZoneName: "shortOffset",
		});
		const parts = formatter.formatToParts(now);
		const offset = parts.find((p) => p.type === "timeZoneName")?.value;
		return offset || "";
	} catch {
		return "";
	}
};

const formatOffset = (tz: string): string => {
	const offset = getOffset(tz);
	if (!offset) return "";
	// Normalise: "GMT+2" → "+2", "GMT-5:30" → "-5:30"
	return offset.replace(/^GMT/, "UTC");
};

export interface TimezoneSelectProps {
	value: string;
	onChange: (value: string) => void;
}

export const TimezoneSelect = ({ value, onChange }: TimezoneSelectProps) => {
	const [opened, setOpened] = useState(false);
	const [search, setSearch] = useState("");

	const allTimezones = useMemo<string[]>(
		() => Intl.supportedValuesOf("timeZone"),
		[],
	);

	const filtered = useMemo(() => {
		if (!search.trim()) return allTimezones;
		const q = search.toLowerCase();
		return allTimezones.filter((tz) => tz.toLowerCase().includes(q));
	}, [allTimezones, search]);

	const detectedTz = useMemo(
		() => Intl.DateTimeFormat().resolvedOptions().timeZone,
		[],
	);

	const detectedOffset = useMemo(() => detectedTz ? formatOffset(detectedTz) : "", [detectedTz]);

	const close = useCallback(() => {
		setOpened(false);
		setSearch("");
	}, []);

	const select = useCallback((tz: string) => {
		onChange(tz);
		close();
	}, [onChange, close]);

	const currentOffset = useMemo(() => formatOffset(value), [value]);

	return (
		<Box>
			<Text style={{ fontSize: 13, color: Colors.TextDimmed, fontWeight: "600", marginBottom: 8, marginLeft: 2 }}>
				Timezone
			</Text>
			<TouchableOpacity
				onPress={() => setOpened(true)}
				activeOpacity={0.8}
				style={{
					backgroundColor: Colors.BackgroundLight,
					borderRadius: 12,
					padding: 14,
					flexDirection: "row",
					alignItems: "center",
				}}
			>
				<Box
					style={{
						width: 40,
						height: 40,
						borderRadius: 10,
						backgroundColor: Colors.PrimaryLight + "33",
						alignItems: "center",
						justifyContent: "center",
						marginRight: 12,
					}}
				>
					<IconWorld size={20} color={Colors.Primary} />
				</Box>
				<Box style={{ flex: 1 }}>
					<Text style={{ fontSize: 15, fontWeight: "600" }}>
						{value}
					</Text>
					<Text style={{ fontSize: 12, color: Colors.TextDimmed, marginTop: 1 }}>
						{currentOffset}
					</Text>
				</Box>
				<IconChevronRight size={18} color={Colors.TextDimmed} />
			</TouchableOpacity>

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
					<Text style={{ fontSize: 13, color: Colors.Primary, fontWeight: "500" }}>
						Use detected: {detectedTz} ({detectedOffset})
					</Text>
				</TouchableOpacity>
			)}

			<Sheet open={opened} onClose={close} height={0.85}>
				<Box p="md" style={{ borderBottomWidth: 1, borderBottomColor: Colors.BackgroundLight }}>
					<Text style={{ fontSize: 18, fontWeight: "bold", marginBottom: 10 }}>
						Select Timezone
					</Text>
					<Box
						style={{
							flexDirection: "row",
							alignItems: "center",
							backgroundColor: Colors.BackgroundLight,
							borderRadius: 10,
							paddingHorizontal: 12,
							gap: 8,
						}}
					>
						<IconSearch size={18} color={Colors.TextDimmed} />
						<TextInput
							value={search}
							onChangeText={setSearch}
							placeholder="Search timezones…"
							placeholderTextColor={Colors.TextDimmed}
							style={{
								flex: 1,
								paddingVertical: 10,
								color: Colors.Text,
								fontSize: 15,
							}}
						/>
					</Box>
				</Box>
				<FlatList
					data={filtered}
					keyExtractor={(item) => item}
					style={{ flex: 1 }}
					keyboardShouldPersistTaps="handled"
					contentContainerStyle={{ paddingVertical: 4 }}
					renderItem={({ item }) => {
						const selected = item === value;
						const offset = formatOffset(item);
						const region = item.split("/")[0] || "";
						return (
							<TouchableOpacity
								onPress={() => select(item)}
								activeOpacity={0.7}
								style={{
									paddingHorizontal: 16,
									paddingVertical: 14,
									backgroundColor: selected ? Colors.PrimaryLight + "33" : "transparent",
									flexDirection: "row",
									alignItems: "center",
									borderRadius: 10,
									marginHorizontal: 8,
									marginVertical: 2,
								}}
							>
								<Box
									style={{
										minWidth: 64,
										paddingHorizontal: 8,
										paddingVertical: 4,
										borderRadius: 6,
										backgroundColor: selected ? Colors.Primary : Colors.BackgroundLight,
										alignItems: "center",
										justifyContent: "center",
										marginRight: 12,
									}}
								>
									<Text style={{ fontSize: 12, fontWeight: "700", color: selected ? "#fff" : Colors.TextDimmed }}>
										{offset || "—"}
									</Text>
								</Box>
								<Box style={{ flex: 1 }}>
									<Text
										style={{ fontSize: 14, fontWeight: selected ? "600" : "400" }}
										numberOfLines={1}
									>
										{item}
									</Text>
									<Text style={{ fontSize: 11, color: Colors.TextDimmed, marginTop: 1, textTransform: "capitalize" }}>
										{region.replace(/_/g, " ")}
									</Text>
								</Box>
								{selected && (
									<IconCheck size={20} color={Colors.Primary} />
								)}
							</TouchableOpacity>
						);
					}}
				/>
			</Sheet>
		</Box>
	);
};
