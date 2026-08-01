import { useCallback } from "react";
import { PartialDateUtil } from "@evnt/partial-date";
import { PartialDate } from "@evnt/types";
import { useLocaleStore } from "@/stores/useLocaleStore";

export interface PartialDateEditorHandlers {
	paddedYear: string;
	paddedMonth: string;
	paddedDay: string;
	paddedHour: string;
	paddedMinute: string;
	timezone: string;
	disabledMonth: boolean;
	disabledDay: boolean;
	disabledTime: boolean;
	handleDateChange: (field: "year" | "month" | "day", raw: string) => void;
	handleTimeChange: (field: "hour" | "minute", raw: string) => void;
	clearDateField: (field: "month" | "day") => void;
	setTimezone: (timezone: string) => void;
}

const EMPTY_YEAR = "----";
const EMPTY_SHORT = "--";
const MIN_MONTH = 1;
const MAX_MONTH = 12;
const MIN_DAY = 1;
const MIN_HOUR = 0;
const MAX_HOUR = 23;
const MIN_MINUTE = 0;
const MAX_MINUTE = 59;

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

const getMaxDay = (year: number, month: number) =>
	new Temporal.PlainYearMonth(year, month, "iso8601").daysInMonth;

const editField = (
	value: PartialDate | undefined,
	formatPD: (parsed: PartialDate.Parsed) => PartialDate,
	patch: {
		year?: number;
		month?: number;
		day?: number;
		hour?: number;
		minute?: number;
		precision?: PartialDate.Precision;
	},
): PartialDate | undefined => {
	if (!value) return undefined;

	const parsed = PartialDateUtil.parse(value) as PartialDate.Parsed.Fields;

	const base = {
		year: patch.year ?? parsed.year,
		month: patch.month ?? parsed.month,
		day: patch.day ?? parsed.day,
		hour: patch.hour ?? parsed.hour,
		minute: patch.minute ?? parsed.minute,
		timezone: parsed.timezone,
	};

	const precision = patch.precision ?? PartialDateUtil.getPrecision(value);

	return formatPD({ ...base, precision } as PartialDate.Parsed);
};

export const usePartialDateEditor = (
	value: PartialDate | undefined,
	onChange: (value: PartialDate | undefined) => void,
): PartialDateEditorHandlers => {
	const userTimezone = useLocaleStore((s) => s.timezone);

	const parsed = value ? (PartialDateUtil.parse(value) as PartialDate.Parsed.Fields) : null;
	const timezone = parsed ? parsed.timezone : userTimezone;

	const padInput = (text: string, width: 2 | 4): string => {
		const digits = text.replace(/\D/g, "");
		if (digits.length === 0) return "";
		if (digits.length < width) return digits.padStart(width, "0");
		return digits.slice(-width);
	};

	const formatPD = (parsed: PartialDate.Parsed): PartialDate => {
		const result = PartialDateUtil.format(parsed);
		return result.replace(/^\d+/, (m) => m.padStart(4, "0")) as PartialDate;
	};

	const hasPrecision = (precision: PartialDate.Precision): boolean => {
		if (!value) return false;
		const order = ["year", "month", "day", "time"] as const;
		return order.indexOf(PartialDateUtil.getPrecision(value)) >= order.indexOf(precision);
	};

	const handleTimeChange = useCallback(
		(field: "hour" | "minute", raw: string) => {
			if (!parsed || !hasPrecision("day")) return;
			const text = padInput(raw, 2);

			if (text === "") {
				// Drop to day precision (clear time)
				onChange(
					editField(value, formatPD, {
						precision: "day",
						day: parsed.day,
					}),
				);
				return;
			}

			const num = clamp(
				parseInt(text, 10),
				field === "hour" ? MIN_HOUR : MIN_MINUTE,
				field === "hour" ? MAX_HOUR : MAX_MINUTE,
			);
			const hour = field === "hour" ? num : parsed.hour;
			const minute = field === "minute" ? num : parsed.minute;

			// Promote to time precision
			onChange(
				editField(value, formatPD, {
					precision: "time",
					hour,
					minute,
				}),
			);
		},
		[value, onChange, formatPD, parsed, hasPrecision, editField],
	);

	const clearDateField = useCallback(
		(field: "month" | "day") => {
			if (!value || !parsed) return;

			if (field === "month") {
				// Clear month: drop to year precision
				onChange(PartialDateUtil.setPrecision(value, "year"));
				return;
			}

			if (hasPrecision("time")) {
				// Clear day on time value: keep time, set day to 1
				onChange(
					editField(value, formatPD, {
						precision: "time",
						day: 1,
					}),
				);
				return;
			}

			// Clear day on day/month-only value
			if (hasPrecision("month")) {
				onChange(PartialDateUtil.setPrecision(value, "month", "low"));
			} else {
				onChange(PartialDateUtil.setPrecision(value, "year"));
			}
		},
		[value, onChange, formatPD, parsed, hasPrecision, editField],
	);

	const handleDateChange = useCallback(
		(field: "year" | "month" | "day", raw: string) => {
			const text = padInput(raw, field === "year" ? 4 : 2);

			if (field !== "year" && (text === "" || text === "00")) {
				if (value) clearDateField(field);
				return;
			}

			if (field === "year" && (text === "" || parseInt(text, 10) === 0)) {
				onChange(undefined);
				return;
			}

			if (!parsed) {
				onChange(
					formatPD({
						precision: "year",
						year: parseInt(text, 10),
						timezone: userTimezone,
					} as PartialDate.Parsed.YearOnly),
				);
				return;
			}

			const num = parseInt(text, 10);

			if (field === "month") {
				// Promotion: year -> month is allowed
				if (!hasPrecision("year")) return;
				const month = clamp(num, MIN_MONTH, MAX_MONTH);

				if (hasPrecision("day")) {
					// Update month on existing day precision
					onChange(editField(value, formatPD, { day: parsed.day, month }));
					return;
				}
				if (hasPrecision("time")) {
					// Update month on existing time precision
					onChange(editField(value, formatPD, { precision: "time", month }));
					return;
				}
				// Promote year-only to month precision
				onChange(editField(value, formatPD, { precision: "month", month }));
				return;
			}

			if (field === "day") {
				// Promotion: month -> day is allowed
				if (!hasPrecision("month")) return;
				const day = clamp(num, MIN_DAY, getMaxDay(parsed.year, parsed.month));

				if (hasPrecision("time")) {
					// Update day on existing time precision
					onChange(editField(value, formatPD, { precision: "time", day }));
					return;
				}
				// Promote month-only to day precision
				onChange(editField(value, formatPD, { precision: "day", day }));
				return;
			}

			// Year: always allowed, no promotion needed
			onChange(editField(value, formatPD, { year: num }));
		},
		[value, onChange, formatPD, parsed, hasPrecision, clearDateField, editField],
	);

	const setTimezone = useCallback(
		(timezone: string) => {
			if (!value) {
				onChange(
					formatPD({
						precision: "year",
						year: new Date().getFullYear(),
						timezone,
					} as PartialDate.Parsed.YearOnly),
				);
				return;
			}
			onChange(PartialDateUtil.withTimezone(value, timezone));
		},
		[value, onChange, formatPD],
	);

	const disabledMonth = !parsed || !hasPrecision("year");
	const disabledDay = !parsed || !hasPrecision("month");
	const disabledTime = !parsed || !hasPrecision("day");

	const paddedYear = parsed ? String(parsed.year) : EMPTY_YEAR;
	const paddedMonth = disabledMonth
		? ""
		: hasPrecision("month")
			? String(parsed!.month).padStart(2, "0")
			: EMPTY_SHORT;
	const paddedDay = disabledDay
		? ""
		: hasPrecision("day")
			? String(parsed!.day).padStart(2, "0")
			: EMPTY_SHORT;
	const paddedHour = disabledTime
		? ""
		: hasPrecision("time")
			? String(parsed!.hour).padStart(2, "0")
			: EMPTY_SHORT;
	const paddedMinute = disabledTime
		? ""
		: hasPrecision("time")
			? String(parsed!.minute).padStart(2, "0")
			: EMPTY_SHORT;

	return {
		paddedYear,
		paddedMonth,
		paddedDay,
		paddedHour,
		paddedMinute,
		timezone,
		disabledMonth,
		disabledDay,
		disabledTime,
		handleDateChange,
		handleTimeChange,
		clearDateField,
		setTimezone,
	};
};
