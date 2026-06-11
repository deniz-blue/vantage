import { useState, useRef, useMemo, useCallback, type Ref } from "react";
import {
	TextInput as RNTextInput,
	type TextInput,
	Modal,
	FlatList,
	TouchableOpacity,
} from "react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Button } from "../base/Button";
import { Colors } from "../../theme/colors";

// === Precision levels ===

const PRECISIONS = [
	{ value: "year", label: "Year" },
	{ value: "month", label: "Month" },
	{ value: "day", label: "Day" },
	{ value: "time", label: "+ Time" },
] as const;

type Precision = (typeof PRECISIONS)[number]["value"];

// === PartialDate Builder ===

export interface DateValue {
	year: string;
	month: string;
	day: string;
	hour: string;
	minute: string;
	timezone: string;
}

export const emptyDate = (): DateValue => {
	const now = new Date();
	return {
		year: String(now.getFullYear()),
		month: "",
		day: "",
		hour: "",
		minute: "",
		timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
	};
};

export const partialDatePreview = (d: DateValue): string | null => {
	if (!d.month || !d.day || !d.year) return null;
	const m = Number(d.month) - 1;
	const day = Number(d.day);
	const y = Number(d.year);
	if (isNaN(m) || isNaN(day) || isNaN(y)) return null;

	const date = new Date(y, m, day);
	const formatter = new Intl.DateTimeFormat(undefined, {
		weekday: "short",
		month: "short",
		day: "numeric",
		year: "numeric",
	});
	let preview = formatter.format(date);

	const hasTime = d.hour.length > 0 && d.minute.length > 0;
	if (hasTime) {
		const h = Number(d.hour);
		const min = Number(d.minute);
		if (!isNaN(h) && !isNaN(min)) {
			const ampm = h >= 12 ? "PM" : "AM";
			const h12 = h === 0 ? 12 : h > 12 ? h - 12 : h;
			preview += `, ${h12}:${String(min).padStart(2, "0")} ${ampm}`;
		}
		preview += ` (${d.timezone})`;
	}

	return preview;
};

export const dateToPartialDate = (d: DateValue): string | undefined => {
	if (!d.year) return undefined;

	let partial = d.year;
	if (d.month) {
		partial += `-${d.month.padStart(2, "0")}`;
		if (d.day) {
			partial += `-${d.day.padStart(2, "0")}`;
			if (d.hour && d.minute) {
				partial += `T${d.hour.padStart(2, "0")}:${d.minute.padStart(2, "0")}`;
			}
		}
	}
	partial += `[${d.timezone}]`;
	return partial;
};

// === Derive precision from value ===

const precisionFromValue = (v: DateValue): Precision => {
	if (v.month && v.day && v.hour && v.minute) return "time";
	if (v.day) return "day";
	if (v.month) return "month";
	return "year";
};

// === DateInput Component ===

interface DateInputProps {
	value: DateValue;
	onChange: (value: DateValue) => void;
}

export const DateInput = ({ value, onChange }: DateInputProps) => {
	const precision = useMemo(() => precisionFromValue(value), [value]);

	const update = (patch: Partial<DateValue>) => onChange({ ...value, ...patch });

	const yearRef = useRef<TextInput>(null);
	const monthRef = useRef<TextInput>(null);
	const dayRef = useRef<TextInput>(null);
	const hourRef = useRef<TextInput>(null);
	const minuteRef = useRef<TextInput>(null);

	const preview = useMemo(() => partialDatePreview(value), [value]);

	const [tzOpen, setTzOpen] = useState(false);
	const [tzSearch, setTzSearch] = useState("");

	const allTimezones = useMemo<string[]>(() => {
		try {
			return Intl.supportedValuesOf("timeZone");
		} catch {
			return [
				"UTC",
				"America/New_York",
				"America/Chicago",
				"America/Denver",
				"America/Los_Angeles",
				"America/Vancouver",
				"Europe/London",
				"Europe/Paris",
				"Europe/Berlin",
				"Europe/Vilnius",
				"Europe/Helsinki",
				"Asia/Tokyo",
				"Asia/Shanghai",
				"Asia/Kolkata",
				"Australia/Sydney",
				"Pacific/Auckland",
			];
		}
	}, []);

	const filteredTimezones = useMemo(() => {
		if (!tzSearch.trim()) return allTimezones;
		const q = tzSearch.toLowerCase();
		return allTimezones.filter(
			(tz) => tz.toLowerCase().includes(q),
		);
	}, [allTimezones, tzSearch]);

	const selectTimezone = useCallback((tz: string) => {
		update({ timezone: tz });
		setTzOpen(false);
		setTzSearch("");
	}, []);

	// === Precision change ===

	const setPrecision = (p: Precision) => {
		// Focus the first field of the chosen precision level
		setTimeout(() => {
			switch (p) {
				case "year": yearRef.current?.focus(); break;
				case "month": monthRef.current?.focus(); break;
				case "day": dayRef.current?.focus(); break;
				case "time": hourRef.current?.focus(); break;
			}
		}, 100);
	};

	// === Input handlers ===

	const handleYear = (v: string) => {
		const clean = v.replace(/\D/g, "").slice(0, 4);
		update({ year: clean });
		if (clean.length >= 4) monthRef.current?.focus();
	};

	const handleMonth = (v: string) => {
		const clean = v.replace(/\D/g, "").slice(0, 2);
		update({ month: clean });
		if (clean.length === 2) {
			if (precision === "month") dayRef.current?.focus();
			setTimeout(() => dayRef.current?.focus(), 50);
		}
	};

	const handleDay = (v: string) => {
		update({ day: v.replace(/\D/g, "").slice(0, 2) });
	};

	const handleHour = (v: string) => {
		const clean = v.replace(/\D/g, "").slice(0, 2);
		update({ hour: clean });
		if (clean.length === 2) minuteRef.current?.focus();
	};

	const handleMinute = (v: string) => {
		update({ minute: v.replace(/\D/g, "").slice(0, 2) });
	};

	const clearDate = () => {
		onChange({
			...value,
			month: "",
			day: "",
			hour: "",
			minute: "",
		});
	};

	return (
		<Box bg={Colors.BackgroundLight} radius={10} p={14} gap={12}>
			{/* Precision indicator */}
			<Box direction="row" gap={4}>
				{PRECISIONS.map((p) => {
					const isActive = precision === p.value;

					return (
						<Button
							key={p.value}
							variant={isActive ? "filled" : "subtle"}
							color={isActive ? Colors.Primary : undefined}
							size="sm"
							style={{ flex: 1, paddingHorizontal: 4 }}
							onPress={() => setPrecision(p.value)}
						>
							{p.label}
						</Button>
					);
				})}
			</Box>

			{/* Date+time row */}
			<Box direction="row" gap={12}>
				{/* Date fields — grouped mask */}
				<Box direction="row" align="center" justify="center" flex={1} bg={Colors.Background} radius={8}>
					<DateField
						ref={yearRef}
						placeholder="YYYY"
						value={value.year}
						onChangeText={handleYear}
						w={56}
						grouped
					/>
					<Text style={{ fontSize: 16, color: Colors.TextDimmed }}>
						/
					</Text>
					<DateField
						ref={monthRef}
						placeholder="MM"
						value={value.month}
						onChangeText={handleMonth}
						w={36}
						grouped
					/>
					<Text style={{ fontSize: 16, color: Colors.TextDimmed }}>
						/
					</Text>
					<DateField
						ref={dayRef}
						placeholder="DD"
						value={value.day}
						onChangeText={handleDay}
						w={36}
						grouped
					/>
				</Box>

				{/* Time fields — grouped mask */}
				<Box direction="row" align="center" justify="center" flex={1} bg={Colors.Background} radius={8}>
					<DateField
						ref={hourRef}
						placeholder="HH"
						value={value.hour}
						onChangeText={handleHour}
						w={36}
						grouped
					/>
					<Text style={{ fontSize: 16, color: Colors.TextDimmed }}>
						:
					</Text>
					<DateField
						ref={minuteRef}
						placeholder="MM"
						value={value.minute}
						onChangeText={handleMinute}
						w={36}
						grouped
					/>
				</Box>
			</Box>

			{/* Date preview */}
			{preview && (
				<Text
					style={{
						fontSize: 13,
						color: Colors.Primary,
						textAlign: "center",
					}}
				>
					{preview}
				</Text>
			)}

			{/* Timezone */}
			<TouchableOpacity onPress={() => setTzOpen(true)} activeOpacity={0.6}>
				<Text style={{ fontSize: 12, color: Colors.Primary }}>
					{value.timezone} ▾
				</Text>
			</TouchableOpacity>

			{/* Clear */}
			<Button
				variant="subtle"
				size="sm"
				color={Colors.TextDimmed}
				onPress={clearDate}
			>
				Clear
			</Button>

			{/* Timezone picker — bottom sheet */}
			<Modal
				visible={tzOpen}
				transparent
				animationType="slide"
				onRequestClose={() => { setTzOpen(false); setTzSearch(""); }}
			>
				<Box flex={1} justify="flex-end">
					<TouchableOpacity
						style={{ flex: 1 }}
						onPress={() => { setTzOpen(false); setTzSearch(""); }}
					/>

					<Box bg={Colors.BackgroundLight} rtl={16} rtr={16} pb={32} mah="70%">
						{/* Search */}
						<Box p={12} gap={8}>
							<Text style={{ fontSize: 16, fontWeight: "600" }}>
								Timezone
							</Text>
							<RNTextInput
								value={tzSearch}
								onChangeText={setTzSearch}
								placeholder="Search timezones..."
								placeholderTextColor={Colors.TextDimmed}
								style={{
									backgroundColor: Colors.Background,
									color: Colors.Text,
									borderRadius: 8,
									paddingHorizontal: 12,
									paddingVertical: 10,
									fontSize: 15,
								}}
								autoFocus
							/>
						</Box>

						{/* List */}
						<FlatList
							data={filteredTimezones}
							keyExtractor={(item) => item}
							initialNumToRender={30}
							style={{ maxHeight: 400 }}
							contentContainerStyle={{ paddingHorizontal: 12, gap: 2 }}
							renderItem={({ item }) => {
								const isSelected = item === value.timezone;
								return (
									<TouchableOpacity
										onPress={() => selectTimezone(item)}
										activeOpacity={0.6}
										style={{
											paddingVertical: 10,
											paddingHorizontal: 12,
											borderRadius: 6,
											backgroundColor: isSelected ? Colors.Primary + "22" : "transparent",
										}}
									>
										<Text
											style={{
												fontSize: 15,
												color: isSelected ? Colors.Primary : Colors.Text,
											}}
										>
											{item}
										</Text>
									</TouchableOpacity>
								);
							}}
						/>
					</Box>
				</Box>
			</Modal>
		</Box>
	);
};

// === DateField helper ===

const DateField = ({
	placeholder,
	value,
	onChangeText,
	ref,
	grouped,
	w,
}: {
	placeholder: string;
	value: string;
	onChangeText: (text: string) => void;
	ref?: Ref<TextInput>;
	grouped?: boolean;
	w?: number;
}) => (
	<Box w={w}>
		<RNTextInput
			ref={ref}
			value={value}
			onChangeText={onChangeText}
			placeholder={placeholder}
			placeholderTextColor={Colors.TextDimmed}
			keyboardType="number-pad"
			style={{
				backgroundColor: grouped ? "transparent" : Colors.Background,
				color: Colors.Text,
				borderRadius: grouped ? 0 : 6,
				paddingHorizontal: 4,
				paddingVertical: 10,
				fontSize: 16,
				textAlign: "center",
				borderWidth: 0,
				width: "100%",
			}}
		/>
	</Box>
);
