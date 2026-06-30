import { PartialDate } from "@evnt/types";
import { Box } from "../../../base/Box";
import { Button } from "../../../base/button/Button";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { useState } from "react";
import { Sheet } from "../../../base/Sheet";
import { Text } from "../../../base/Text";
import { PartialDateUtil } from "@evnt/partial-date";
import { TimezoneSelect } from "../../../core/timezone-select";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { IconCalendar, IconCheck, IconChevronRight } from "@tabler/icons-react-native";
import { FontSize, IconSize, Radius } from "../../../../theme/sizing";
import { ActionIcon } from "../../../base/button/ActionIcon";
import { Colors } from "../../../../theme/colors";
import { SegmentedTextInput } from "../../../base/input/SegmentedTextInput";
import { formatDate } from "@evnt/pretty";

export interface PartialDateInputProps {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
	label?: string;
}

export const PartialDateInput = ({
	value,
	onChange,
	label,
}: PartialDateInputProps) => {
	const [open, setOpen] = useState(false);
	const language = useLocaleStore((s) => s.language);
	const tz = useLocaleStore((s) => s.timezone);

	const hasTime = value ? PartialDateUtil.parse(value).precision === "time" : false;

	const fields = PartialDateUtil.parse(value ?? "2000[UTC]") as PartialDate.Parsed.Fields;

	const padInput = (text: string, width: 2 | 4): string => {
		const digits = text.replace(/\D/g, "");
		if (digits.length === 0) return "";
		if (digits.length < width) return digits.padStart(width, "0");
		return digits.slice(-width);
	};

	const formatPD = (parsed: PartialDate.Parsed): PartialDate => {
		const result = PartialDateUtil.format(parsed);
		return result.replace(/^\d+/, m => m.padStart(4, "0")) as PartialDate;
	};

	const handleTimeChange = (field: "hour" | "minute", raw: string) => {
		if (!value) return;
		const f = PartialDateUtil.parse(value) as PartialDate.Parsed.Fields;
		const text = padInput(raw, 2);

		if (text === "") {
			onChange(formatPD({
				precision: "day",
				year: f.year, month: f.month, day: f.day,
				timezone: f.timezone,
			} as PartialDate.Parsed.YearMonthDay));
			return;
		}

		const num = parseInt(text, 10);
		let hour = f.hour;
		let minute = f.minute;

		switch (field) {
			case "hour": hour = num; break;
			case "minute": minute = num; break;
		}

		onChange(formatPD({
			precision: "time",
			year: f.year, month: f.month, day: f.day,
			hour, minute,
			timezone: f.timezone,
		} as PartialDate.Parsed.YearMonthDayTime));
	};

	const clearDateField = (field: "month" | "day") => {
		if (!value) return;
		const f = PartialDateUtil.parse(value) as PartialDate.Parsed.Fields;
		const hasTime = PartialDateUtil.has(value, "time");
		const hasMonth = PartialDateUtil.has(value, "month");

		switch (true) {
			case field === "month": {
				onChange(PartialDateUtil.lowerPrecision(
					formatPD({
						...(hasTime
							? { precision: "time" as const, day: 1 }
							: { precision: "year" as const, month: 1, day: 1 }),
						...f,
					} as PartialDate.Parsed) as PartialDate.YearOnly,
					"year",
				));
			} break;

			case field === "day" && hasTime: {
				onChange(formatPD({
					precision: "time",
					year: f.year, month: f.month, day: 1,
					hour: f.hour, minute: f.minute,
					timezone: f.timezone,
				} as PartialDate.Parsed.YearMonthDayTime));
			} break;

			case field === "day" && hasMonth: {
				onChange(PartialDateUtil.lowerPrecision(
					formatPD({ ...f, precision: "month" } as PartialDate.Parsed) as PartialDate.YearMonth,
					"month",
				));
			} break;

			case field === "day": {
				onChange(PartialDateUtil.lowerPrecision(
					formatPD({ ...f, precision: "year" } as PartialDate.Parsed) as PartialDate.YearOnly,
					"year",
				));
			} break;
		}
	};

	const handleDateChange = (field: "year" | "month" | "day", raw: string) => {
		const text = padInput(raw, field === "year" ? 4 : 2);

		if (field !== "year" && (text === "" || text === "00")) {
			if (value) clearDateField(field);
			return;
		}

		if (field === "year" && (text === "" || parseInt(text, 10) === 0)) {
			onChange(undefined);
			return;
		}

		if (!value) {
			onChange(formatPD({
				precision: "year",
				year: parseInt(text, 10),
				timezone: tz,
			} as PartialDate.Parsed));
			return;
		}

		const f = PartialDateUtil.parse(value) as PartialDate.Parsed.Fields;
		const num = parseInt(text, 10);

		const currentPrecision = PartialDateUtil.getPrecision(value);
		const hasMonth = PartialDateUtil.has(value, "month");
		const hasDay = PartialDateUtil.has(value, "day");

		let year = f.year;
		let month = hasMonth ? f.month : 1;
		let day = hasDay ? f.day : 1;

		switch (field) {
			case "year": year = num; break;
			case "month": month = num; if (hasDay) day = f.day; break;
			case "day": day = num; month = f.month; break;
		}

		const precision: PartialDate.Precision = (() => {
			switch (true) {
				case currentPrecision === "time": return "time";
				case field === "day": return "day";
				case field === "month": return hasDay ? "day" : "month";
				default: return currentPrecision;
			}
		})();

		const base = {
			year,
			month: precision === "year" ? 1 : month,
			day: precision === "day" || precision === "time" ? day : 1,
			timezone: f.timezone,
		};

		if (PartialDateUtil.has(value, "time")) {
			onChange(formatPD({
				...base,
				precision: "time",
				hour: f.hour, minute: f.minute,
			} as PartialDate.Parsed.YearMonthDayTime));
		} else {
			onChange(formatPD({
				...base,
				precision,
			} as PartialDate.Parsed));
		}
	};

	const handleTimeToggle = () => {
		if (hasTime) {
			if (!value) return;
			const parsed = PartialDateUtil.parse(value);
			onChange(PartialDateUtil.lowerPrecision(
				formatPD(parsed) as PartialDate.YearMonthDayTime,
				"day",
			));
			return;
		}

		const f = value
			? PartialDateUtil.parse(value) as PartialDate.Parsed.Fields
			: { year: new Date().getFullYear(), month: 1, day: 1, hour: 0, minute: 0, timezone: tz };

		const hasDay = value ? PartialDateUtil.has(value, "day") : false;
		const hasMonth = value ? PartialDateUtil.has(value, "month") : false;

		onChange(formatPD({
			precision: "time",
			year: f.year,
			month: hasMonth ? f.month : 1,
			day: hasDay ? f.day : 1,
			hour: 0,
			minute: 0,
			timezone: f.timezone,
		} as PartialDate.Parsed.YearMonthDayTime));
	};

	const colonNormal = <Text fz={FontSize.md}>:</Text>;
	const colonDimmed = <Text fz={FontSize.md} c={Colors.TextDimmed}>:</Text>;
	const slashNormal = <Text fz={FontSize.md}>/</Text>;
	const slashDimmed = <Text fz={FontSize.md} c={Colors.TextDimmed}>/</Text>;

	return (
		<Box>
			<InputWrapper
				label={label}
			>
				<Button
					onPress={() => setOpen(true)}
					rightSection={<IconChevronRight size={IconSize.xs} color={Colors.TextDimmed} />}
				>
					<Text fz={FontSize.sm} c={value ? undefined : Colors.TextDimmed}>
						{value ? formatDate(value, { language, timezone: tz, compactDates: false }) : "Unknown"}
					</Text>
				</Button>
			</InputWrapper>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box gap="md" p="md">
					<Box gap="xs">
						<Box direction="row" justify="space-between" align="center">
							<InputWrapper label="Date" />
							<Box direction="row" gap="sm">
								<Button
									py={0}
									mih={null}
									variant="subtle"
									onPress={handleTimeToggle}
									rightSection={(
										<Box
											w={IconSize.xs}
											h={IconSize.xs}
											justify="center"
											align="center"
											style={{
												borderColor: hasTime ? Colors.Primary : Colors.Dark1,
												backgroundColor: hasTime ? Colors.Primary : undefined,
												borderWidth: 2,
												borderRadius: Radius.xs,
											}}
										>
											{hasTime && <IconCheck size={IconSize.xs} color={Colors.White} strokeWidth={3} />}
										</Box>
									)}
								>
									<InputWrapper label="Time" />
								</Button>
							</Box>
						</Box>

						<Box direction="row" flex={1} gap="md" justify="space-between" align="center">
							<Box direction="row" gap="sm">
								<ActionIcon>
									<IconCalendar />
								</ActionIcon>

								<SegmentedTextInput
									segments={[
										{
											value: value ? fields.year.toString() : "",
											onChangeText: text => handleDateChange("year", text),
											style: { width: 44 },
											placeholder: "----",
											renderAfter: value && PartialDateUtil.has(value, "month") ? slashNormal : slashDimmed,
										},
										{
											value: value && PartialDateUtil.has(value, "month") ? fields.month.toString().padStart(2, "0") : "",
											onChangeText: text => handleDateChange("month", text),
											readOnly: !value || !PartialDateUtil.has(value, "year"),
											focusable: value && PartialDateUtil.has(value, "year"),
											renderAfter: value && PartialDateUtil.has(value, "day") ? slashNormal : slashDimmed,
										},
										{
											value: value && PartialDateUtil.has(value, "day") ? fields.day.toString().padStart(2, "0") : "",
											onChangeText: text => handleDateChange("day", text),
											readOnly: !value || !PartialDateUtil.has(value, "month"),
											focusable: value && PartialDateUtil.has(value, "month"),
										}
									]}
									common={{
										style: { width: 22 },
										selectTextOnFocus: true,
										placeholder: "--",
										keyboardType: "decimal-pad",
									}}
									separator={<Text fz={FontSize.md}>/</Text>}
								/>
							</Box>

							<SegmentedTextInput
								segments={[
									{
										value: hasTime ? fields.hour.toString().padStart(2, "0") : "",
										onChangeText: text => handleTimeChange("hour", text),
										renderAfter: hasTime ? colonNormal : colonDimmed,
									},
									{
										value: hasTime ? fields.minute.toString().padStart(2, "0") : "",
										onChangeText: text => handleTimeChange("minute", text),
									}
								]}
								common={{
									style: { width: 22, textAlign: "center" },
									selectTextOnFocus: true,
									placeholder: "--",
									keyboardType: "decimal-pad",
									readOnly: !hasTime,
									focusable: hasTime,
								}}
							/>
						</Box>
					</Box>

					<TimezoneSelect
						variant="form"
						label="Timezone"
						value={value ? PartialDateUtil.parse(value).timezone : ""}
						onChange={timezone => onChange(PartialDateUtil.format({
							...(value ? PartialDateUtil.parse(value) : {
								precision: "year",
								year: new Date().getFullYear(),
							}),
							timezone,
						}))}
					/>

					<Box direction="row" justify="flex-end" gap="sm">
						<Button
							variant="primary"
							onPress={() => setOpen(false)}
							leftSection={<IconCheck size={IconSize.sm} />}
						>
							Done
						</Button>
					</Box>
				</Box>
			</Sheet>
		</Box >
	);
};
