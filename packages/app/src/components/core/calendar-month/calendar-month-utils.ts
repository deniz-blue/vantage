export interface CalendarDay {
	year: number;
	month: number;
	day: number;
	isOutsideMonth: boolean;
	isToday: boolean;
}

export interface CalendarGrid {
	days: CalendarDay[];
	/** 0-based column index for the first day of the month */
	startOffset: number;
	totalWeeks: number;
}

/** Get an ISO week day where 0=Mon ... 6=Sun */
export function isoWeekday(year: number, month: number, day: number): number {
	return (new Temporal.PlainDate(year, month, day).dayOfWeek + 6) % 7;
}

export function getCalendarGrid(
	year: number,
	month: number,
	firstDayOfWeek: 0 | 1 = 1,
): CalendarGrid {
	const daysInMonth = new Temporal.PlainYearMonth(year, month).daysInMonth;
	const startDow = isoWeekday(year, month, 1);
	const offset = (startDow - firstDayOfWeek + 7) % 7;

	const today = Temporal.Now.plainDateISO();

	const days: CalendarDay[] = [];

	// Padding from previous month
	const prevMonth = month === 1 ? 12 : month - 1;
	const prevYear = month === 1 ? year - 1 : year;
	const daysInPrevMonth = new Temporal.PlainYearMonth(prevYear, prevMonth).daysInMonth;

	for (let i = offset - 1; i >= 0; i--) {
		const d = daysInPrevMonth - i;
		days.push({
			year: prevYear,
			month: prevMonth,
			day: d,
			isOutsideMonth: true,
			isToday: false,
		});
	}

	// Current month
	for (let d = 1; d <= daysInMonth; d++) {
		days.push({
			year,
			month,
			day: d,
			isOutsideMonth: false,
			isToday:
				today.year === year && today.month === month && today.day === d,
		});
	}

	// Padding from next month
	const remaining = 7 - (days.length % 7 || 7);
	const nextMonth = month === 12 ? 1 : month + 1;
	const nextYear = month === 12 ? year + 1 : year;

	for (let d = 1; d <= remaining; d++) {
		days.push({
			year: nextYear,
			month: nextMonth,
			day: d,
			isOutsideMonth: true,
			isToday: false,
		});
	}

	const totalWeeks = days.length / 7;

	return { days, startOffset: offset, totalWeeks };
}
