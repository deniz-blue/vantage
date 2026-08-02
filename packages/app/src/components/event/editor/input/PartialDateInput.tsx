import { PartialDate } from "@evnt/types";
import { Box } from "../../../base/Box";
import { Button } from "../../../base/button/Button";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { useCallback, useMemo, useRef } from "react";
import { Sheet, SheetRef } from "../../../base/sheet/Sheet";
import { Text } from "../../../base/Text";
import { PartialDateUtil } from "@evnt/partial-date";
import { TimezoneSelect } from "../../../core/timezone-select";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { IconCheck, IconChevronRight, IconReload } from "@tabler/icons-react-native";
import { FontSize, IconSize } from "../../../../theme/sizing";
import { ActionIcon } from "../../../base/button/ActionIcon";
import { Colors } from "../../../../theme/colors";
import { SegmentedTextInput } from "../../../base/input/SegmentedTextInput";
import { formatDate } from "@evnt/pretty";
import { PartialDateLabel } from "../../../core/PartialDateLabel";

export interface PartialDateInputProps {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
	label?: string;
	disabled?: boolean;
}

export const PartialDateInput = ({ value, onChange, label, disabled }: PartialDateInputProps) => {
	const sheet = useRef<SheetRef>(null);
	const language = useLocaleStore((s) => s.language);
	const userTimezone = useLocaleStore((s) => s.timezone);

	const onDone = useCallback(() => {
		sheet.current?.dismiss();
	}, []);

	return (
		<Box>
			<InputWrapper label={label}>
				<Button
					onPress={() => sheet.current?.present()}
					rightSection={<IconChevronRight size={IconSize.xs} color={Colors.TextDimmed} />}
					disabled={disabled}
				>
					<Text fz={FontSize.sm} c={value ? undefined : Colors.TextDimmed}>
						{value
							? formatDate(value, { language, timezone: userTimezone, compactDates: false })
							: "Unknown"}
					</Text>
				</Button>
			</InputWrapper>

			<Sheet ref={sheet}>
				<Box gap="md">
					<Box direction="row" gap="md" justify="space-between" align="center">
						<PartialDateInputSectionDate value={value} onChange={onChange} />
						<PartialDateInputSectionTime value={value} onChange={onChange} />
					</Box>

					<PartialDateInputSectionTimezone value={value} onChange={onChange} />
					<PartialDateInputSectionBottom value={value} onDone={onDone} />
				</Box>
			</Sheet>
		</Box>
	);
};

const charWidth = 14;
const colonNormal = <Text fz={FontSize.md}>:</Text>;
const colonDimmed = (
	<Text fz={FontSize.md} c={Colors.TextDimmed}>
		:
	</Text>
);
const slashNormal = <Text fz={FontSize.md}>/</Text>;
const slashDimmed = (
	<Text fz={FontSize.md} c={Colors.TextDimmed}>
		/
	</Text>
);

export type AnyParsedPartialDate = Partial<
	PartialDate.Parsed.Fields & { precision: PartialDate.Precision }
>;

const formatWithYearFix = (parsed: PartialDate.Parsed): PartialDate => {
	const result = PartialDateUtil.format(parsed);
	return result.replace(/^\d+/, (m) => m.padStart(4, "0")) as PartialDate;
};

export const PartialDateInputSectionDate = ({
	value,
	onChange,
}: {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
}) => {
	const parsed = useMemo(() => {
		if (!value) return null;
		return PartialDateUtil.parse(value) as AnyParsedPartialDate;
	}, [value]);

	const paddedYear = useMemo(() => {
		const year = parsed?.year ?? null;
		if (!year) return "----";
		return String(year).padStart(4, "0");
	}, [parsed?.year ?? null]);

	const paddedMonth = useMemo(() => {
		if (!parsed) return "";
		if (parsed.precision === "year") return "--";
		return String(parsed.month).padStart(2, "0");
	}, [parsed?.month ?? null, parsed?.precision ?? null]);

	const paddedDay = useMemo(() => {
		if (!parsed || parsed.precision == "year") return "";
		if (parsed.precision === "month") return "--";
		return String(parsed.day).padStart(2, "0");
	}, [parsed?.day ?? null, parsed?.precision ?? null]);

	const handleYearChange = useCallback(
		(text: string) => {
			let year = parseInt(text.replace(/-/g, ""), 10);
			if (isNaN(year)) return;
			if (!text || !year) return onChange(undefined);
			year = Math.max(1, Math.min(9999, year));
			if (!value) {
				const timezone = useLocaleStore.getState().timezone;
				onChange(formatWithYearFix({ precision: "year", year, timezone }));
			} else {
				const parsed = PartialDateUtil.parse(value);
				parsed.year = year;
				onChange(formatWithYearFix(parsed));
			}
		},
		[value, onChange],
	);

	const handleMonthChange = useCallback(
		(text: string) => {
			let month = parseInt(text.replace(/-/g, ""), 10);
			if (isNaN(month)) return;
			if (!text || !month) return onChange(PartialDateUtil.lowerPrecision(value!, "year"));
			month = Math.max(1, Math.min(12, month));
			if (PartialDateUtil.getPrecision(value!) === "year") {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.month = month;
				parsed.precision = "month";
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			} else {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.month = month;
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			}
		},
		[value, onChange],
	);

	const handleDayChange = useCallback(
		(text: string) => {
			let day = parseInt(text.replace(/-/g, ""), 10);
			if (isNaN(day)) return;
			if (!text || !day)
				return onChange(PartialDateUtil.lowerPrecision(value! as PartialDate.YearMonth, "month"));
			const maxDays = PartialDateUtil.asPlainYearMonth(value as PartialDate.YearMonth).daysInMonth;
			day = Math.max(1, Math.min(maxDays, day));
			if (PartialDateUtil.getPrecision(value!) === "month") {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.day = day;
				parsed.precision = "day";
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			} else {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.day = day;
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			}
		},
		[value, onChange],
	);

	const disabledMonth = !parsed;
	const disabledDay = !parsed || parsed.precision === "year";

	return (
		<Box>
			<InputWrapper label="Date" />
			<SegmentedTextInput
				segments={[
					{
						value: paddedYear,
						onChangeText: handleYearChange,
						style: { width: 4 * charWidth },
						placeholder: "----",
						renderAfter: value
							? PartialDateUtil.has(value, "month")
								? slashNormal
								: slashDimmed
							: slashDimmed,
					},
					{
						value: paddedMonth,
						onChangeText: handleMonthChange,
						focusable: !disabledMonth,
						readOnly: disabledMonth,
						renderAfter: value
							? PartialDateUtil.has(value, "day")
								? slashNormal
								: slashDimmed
							: slashDimmed,
					},
					{
						value: paddedDay,
						onChangeText: handleDayChange,
						focusable: !disabledDay,
						readOnly: disabledDay,
					},
				]}
				common={{
					style: { width: 2 * charWidth, textAlign: "center" },
					selectTextOnFocus: true,
					placeholder: "--",
					keyboardType: "decimal-pad",
				}}
			/>
		</Box>
	);
};

export const PartialDateInputSectionTime = ({
	value,
	onChange,
}: {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
}) => {
	const parsed = useMemo(() => {
		if (!value) return null;
		return PartialDateUtil.parse(value) as AnyParsedPartialDate;
	}, [value]);

	const paddedHour = useMemo(() => {
		if (!parsed || parsed.precision === "year" || parsed.precision === "month") return "";
		if (parsed.precision === "day") return "--";
		return String(parsed.hour).padStart(2, "0");
	}, [parsed?.hour ?? null, parsed?.precision ?? null]);

	const paddedMinute = useMemo(() => {
		if (!parsed || parsed.precision === "year" || parsed.precision === "month") return "";
		if (parsed.precision === "day") return "--";
		return String(parsed.minute).padStart(2, "0");
	}, [parsed?.minute ?? null, parsed?.precision ?? null]);

	const handleHourChange = useCallback(
		(text: string) => {
			let hour = parseInt(text.replace(/-/g, ""), 10);
			if (isNaN(hour)) return;
			if (!text || !hour)
				return onChange(
					PartialDateUtil.lowerPrecision(value! as PartialDate.YearMonthDayTime, "day"),
				);
			hour = Math.max(0, Math.min(23, hour));
			if (PartialDateUtil.getPrecision(value!) === "day") {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.hour = hour;
				parsed.precision = "time";
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			} else {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.hour = hour;
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			}
		},
		[value, onChange],
	);

	const handleMinuteChange = useCallback(
		(text: string) => {
			let minute = parseInt(text.replace(/-/g, ""), 10);
			if (isNaN(minute)) return;
			if (!text || !minute)
				return onChange(
					PartialDateUtil.lowerPrecision(value! as PartialDate.YearMonthDayTime, "day"),
				);
			minute = Math.max(0, Math.min(59, minute));
			if (PartialDateUtil.getPrecision(value!) === "day") {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.minute = minute;
				parsed.precision = "time";
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			} else {
				const parsed = PartialDateUtil.parse(value!) as AnyParsedPartialDate;
				parsed.minute = minute;
				onChange(PartialDateUtil.format(parsed as PartialDate.Parsed));
			}
		},
		[value, onChange],
	);

	const disabledTime = !parsed || parsed.precision === "year" || parsed.precision === "month";

	return (
		<Box>
			<InputWrapper label="Time" />
			<SegmentedTextInput
				segments={[
					{
						value: paddedHour,
						onChangeText: handleHourChange,
						renderAfter: !disabledTime ? colonNormal : colonDimmed,
					},
					{
						value: paddedMinute,
						onChangeText: handleMinuteChange,
					},
				]}
				common={{
					style: { width: 2 * charWidth, textAlign: "center" },
					selectTextOnFocus: true,
					placeholder: "--",
					keyboardType: "decimal-pad",
					readOnly: disabledTime,
					focusable: !disabledTime,
				}}
			/>
		</Box>
	);
};

export const PartialDateInputSectionTimezone = ({
	value,
	onChange,
}: {
	value: PartialDate | undefined;
	onChange: (value: PartialDate | undefined) => void;
}) => {
	const userTimezone = useLocaleStore((s) => s.timezone);
	const timezone = value ? PartialDateUtil.parse(value).timezone : userTimezone;

	const setTimezone = useCallback(
		(timezone: string) => {
			if (!value) {
				onChange(
					PartialDateUtil.format({
						precision: "year",
						year: new Date().getUTCFullYear(),
						timezone,
					}),
				);
			} else {
				onChange(PartialDateUtil.withTimezone(value, timezone));
			}
		},
		[value, onChange],
	);

	return (
		<Box gap="xs">
			<Box direction="row" justify="space-between">
				<InputWrapper label="Timezone" />
				<InputWrapper label="Reset" />
			</Box>
			<Box direction="row" gap="sm">
				<Box flex={1}>
					<TimezoneSelect
						variant="form"
						value={value ? PartialDateUtil.parse(value).timezone : ""}
						onChange={(timezone) => setTimezone(timezone)}
					/>
				</Box>
				<ActionIcon
					disabled={!value || timezone === userTimezone}
					onPress={() => setTimezone(userTimezone)}
				>
					<IconReload color={Colors.Text} />
				</ActionIcon>
			</Box>
		</Box>
	);
};

export const PartialDateInputSectionBottom = ({
	value,
	onDone,
}: {
	value: PartialDate | undefined;
	onDone?: () => void;
}) => {
	return (
		<Box direction="row" justify="space-between" gap="xs">
			<Box>
				<InputWrapper label="Preview" />
				<Box direction="row" gap="sm">
					{value ? <PartialDateLabel value={value} /> : <Text c="TextDimmed">Unknown date</Text>}
				</Box>
			</Box>
			<Button onPress={onDone} leftSection={<IconCheck size={IconSize.sm} color={Colors.Text} />}>
				Done
			</Button>
		</Box>
	);
};
