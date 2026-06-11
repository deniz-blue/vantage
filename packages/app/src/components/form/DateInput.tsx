import { useState } from "react";
import { View, TextInput as RNTextInput, TouchableOpacity } from "react-native";
import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { Colors } from "../../theme/colors";

// === PartialDate Builder ===

export interface DateValue {
	year: string;
	month: string;
	day: string;
	hour: string;
	minute: string;
	hasTime: boolean;
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
		hasTime: false,
		timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
	};
};

export const dateToPartialDate = (d: DateValue): string | undefined => {
	if (!d.year) return undefined;

	let partial = d.year;
	if (d.month) {
		partial += `-${d.month.padStart(2, "0")}`;
		if (d.day) {
			partial += `-${d.day.padStart(2, "0")}`;
			if (d.hasTime && d.hour && d.minute) {
				partial += `T${d.hour.padStart(2, "0")}:${d.minute.padStart(2, "0")}`;
			}
		}
	}
	partial += `[${d.timezone}]`;
	return partial;
};

// === DateInput Component ===

interface DateInputProps {
	value: DateValue;
	onChange: (value: DateValue) => void;
}

export const DateInput = ({ value, onChange }: DateInputProps) => {
	const update = (patch: Partial<DateValue>) => onChange({ ...value, ...patch });

	return (
		<Box>
			{/* Date row */}
			<View style={{ flexDirection: "row", gap: 8, alignItems: "flex-end" }}>
				<DateField
					label="Month"
					placeholder="MM"
					maxLength={2}
					value={value.month}
					onChangeText={(v) => update({ month: v.replace(/\D/g, "").slice(0, 2) })}
					style={{ flex: 1 }}
				/>
				<DateSeparator />
				<DateField
					label="Day"
					placeholder="DD"
					maxLength={2}
					value={value.day}
					onChangeText={(v) => update({ day: v.replace(/\D/g, "").slice(0, 2) })}
					style={{ flex: 1 }}
				/>
				<DateSeparator />
				<DateField
					label="Year"
					placeholder="YYYY"
					maxLength={4}
					value={value.year}
					onChangeText={(v) => update({ year: v.replace(/\D/g, "").slice(0, 4) })}
					style={{ flex: 1.5 }}
				/>
			</View>

			{/* Has time toggle */}
			<TouchableOpacity
				onPress={() => update({ hasTime: !value.hasTime })}
				style={{ marginTop: 8 }}
			>
				<Box
					style={{
						paddingVertical: 8,
						paddingHorizontal: 12,
						borderRadius: 6,
						backgroundColor: value.hasTime ? Colors.Primary + "22" : Colors.BackgroundLight,
						borderWidth: 1,
						borderColor: value.hasTime ? Colors.Primary : "transparent",
					}}
				>
					<Text style={{ fontSize: 14, color: value.hasTime ? Colors.Primary : Colors.TextDimmed }}>
						{value.hasTime ? "☑ Include time" : "☐ Include time"}
					</Text>
				</Box>
			</TouchableOpacity>

			{/* Time row */}
			{value.hasTime && (
				<View style={{ flexDirection: "row", gap: 8, alignItems: "flex-end", marginTop: 8 }}>
					<DateField
						label="Hour"
						placeholder="HH (24h)"
						maxLength={2}
						value={value.hour}
						onChangeText={(v) => update({ hour: v.replace(/\D/g, "").slice(0, 2) })}
						style={{ flex: 1 }}
					/>
					<Text style={{ fontSize: 20, paddingBottom: 8 }}>:</Text>
					<DateField
						label="Minute"
						placeholder="MM"
						maxLength={2}
						value={value.minute}
						onChangeText={(v) => update({ minute: v.replace(/\D/g, "").slice(0, 2) })}
						style={{ flex: 1 }}
					/>
				</View>
			)}

			{/* Timezone hint */}
			{value.hasTime && value.month && value.day && (
				<Text style={{ fontSize: 11, color: Colors.TextDimmed, marginTop: 4 }}>
					Timezone: {value.timezone}
				</Text>
			)}
		</Box>
	);
};

// === DateField helper ===

const DateField = ({
	label,
	placeholder,
	maxLength,
	value,
	onChangeText,
	style,
}: {
	label: string;
	placeholder: string;
	maxLength: number;
	value: string;
	onChangeText: (text: string) => void;
	style?: any;
}) => (
	<View style={style}>
		<Text style={{ fontSize: 11, color: Colors.TextDimmed, marginBottom: 4 }}>
			{label}
		</Text>
		<RNTextInput
			value={value}
			onChangeText={onChangeText}
			placeholder={placeholder}
			placeholderTextColor={Colors.TextDimmed}
			maxLength={maxLength}
			keyboardType="number-pad"
			style={{
				backgroundColor: Colors.BackgroundLight,
				color: Colors.Text,
				borderRadius: 6,
				paddingHorizontal: 12,
				paddingVertical: 10,
				fontSize: 16,
				textAlign: "center",
				borderWidth: 1,
				borderColor: "transparent",
			}}
		/>
	</View>
);

const DateSeparator = () => (
	<Text style={{ fontSize: 20, paddingBottom: 8, color: Colors.TextDimmed }}>
		/
	</Text>
);
