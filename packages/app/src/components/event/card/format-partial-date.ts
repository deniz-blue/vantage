import { PartialDateUtil } from "@evnt/partial-date";

/**
 * Compact day/month label from a PartialDate, e.g. "Jun 15", "Nov 2026", "2026".
 * Omits year when it's the current year for brevity.
 */
export const formatDateLabel = (pd: string, locale: string): string => {
	const p = PartialDateUtil.parse(pd as any);
	switch (p.precision) {
		case "year":
			return String(p.year);
		case "month": {
			const d = new Date(Date.UTC(p.year, p.month - 1));
			return d.toLocaleDateString(locale, { year: "numeric", month: "short", timeZone: "UTC" });
		}
		case "day":
		case "time": {
			const now = new Date();
			const d = new Date(Date.UTC(p.year, p.month - 1, p.day));
			return d.toLocaleDateString(locale, {
				month: "short",
				day: "numeric",
				...(p.year !== now.getUTCFullYear() ? { year: "numeric" } : {}),
				timeZone: "UTC",
			});
		}
	}
};

/**
 * Time label from a PartialDate, e.g. "09:00" or "9:00 AM".
 * Returns null if the date has no time component.
 */
export const formatTimeLabel = (pd: string, locale: string): string | null => {
	if (!PartialDateUtil.has(pd as any, "time")) return null;
	const p = PartialDateUtil.parse(pd as any);
	if (p.precision !== "time") return null;

	const d = new Date(Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute));
	return d.toLocaleTimeString(locale, {
		hour: "2-digit",
		minute: "2-digit",
		hour12: false,
		timeZone: "UTC",
	});
};

/**
 * Full time range label, e.g. "09:00–17:00".
 */
export const formatTimeRange = (
	start: string | undefined,
	end: string | undefined,
	locale: string,
): string | null => {
	const startTime = start ? formatTimeLabel(start, locale) : null;
	const endTime = end ? formatTimeLabel(end, locale) : null;

	if (startTime && endTime) return `${startTime}–${endTime}`;
	if (startTime) return startTime;
	if (endTime) return endTime;
	return null;
};

/**
 * Date range between two PartialDates, e.g. "Jun 15–17" or "Jun 15 – Jul 1".
 */
export const formatDateRange = (
	start: string | undefined,
	end: string | undefined,
	locale: string,
): string | null => {
	if (!start) return end ? formatDateLabel(end, locale) : null;
	if (!end) return formatDateLabel(start, locale);

	const startLabel = formatDateLabel(start, locale);
	const endLabel = formatDateLabel(end, locale);

	return startLabel === endLabel
		? startLabel
		: `${startLabel}–${endLabel}`;
};
