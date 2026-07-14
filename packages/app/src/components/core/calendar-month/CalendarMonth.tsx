import { useMemo } from "react";
import type { ReactNode } from "react";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { FontSize, Radius } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";
import { getCalendarGrid, type CalendarDay } from "./calendar-month-utils";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import type { Spacing } from "../../../theme/shorthand";

const defaultRenderWeekdayHeader = (name: string) => (
	<Box flex={1} align="center" py={4}>
		<Text fz={FontSize.xs} ta="center" c="TextDimmed">
			{name}
		</Text>
	</Box>
);

const defaultRenderDay = (day: CalendarDay) => (
	<Box
		flex={1}
		aspectRatio={1}
		align="center"
		justify="center"
		bg={day.isToday ? Colors.Primary : undefined}
		radius={Radius.sm}
	>
		<Text fz={FontSize.sm} c={day.isToday ? "White" : day.isOutsideMonth ? "TextDimmed" : "Text"}>
			{day.day}
		</Text>
	</Box>
);

export const CalendarMonth = ({
	year,
	month,
	firstDayOfWeek = 1,
	gap,
	renderDay = defaultRenderDay,
	renderWeekdayHeader = defaultRenderWeekdayHeader,
}: {
	year: number;
	month: number;
	firstDayOfWeek?: 0 | 1;
	gap?: Spacing;
	renderDay?: (day: CalendarDay) => ReactNode;
	renderWeekdayHeader?: (dayName: string, index: number) => ReactNode;
}) => {
	const locale = useLocaleStore((s) => s.language);
	const grid = useMemo(
		() => getCalendarGrid(year, month, firstDayOfWeek),
		[year, month, firstDayOfWeek],
	);

	const weekdayNames = useMemo(() => {
		const fmt = new Intl.DateTimeFormat(locale, { weekday: "short" });
		// 2024-01-01 is a Monday (ISO)
		return Array.from({ length: 7 }, (_, i) => {
			const date = new Temporal.PlainDate(2024, 1, 1);
			const shifted = date.add({ days: i });
			return fmt.format(shifted);
		});
	}, [locale]);

	// Rotate weekday names so the grid starts on firstDayOfWeek
	const orderedWeekdays = useMemo(() => {
		const rotated = [...weekdayNames];
		if (firstDayOfWeek === 0) {
			// Move Sunday to front: Sun, Mon, Tue, Wed, Thu, Fri, Sat
			const sun = rotated.pop()!;
			rotated.unshift(sun);
		}
		return rotated;
	}, [weekdayNames, firstDayOfWeek]);

	const weeks = useMemo(() => {
		const result: CalendarDay[][] = [];
		for (let i = 0; i < grid.days.length; i += 7) {
			result.push(grid.days.slice(i, i + 7));
		}
		return result;
	}, [grid.days]);

	return (
		<Box gap={gap}>
			{/* Weekday headers */}
			<Box direction="row">
				{orderedWeekdays.map((name, i) => (
					<Box flex={1} key={i}>
						{renderWeekdayHeader(name, i)}
					</Box>
				))}
			</Box>

			{/* Day grid */}
			{weeks.map((week, wi) => (
				<Box direction="row" gap={gap} key={wi}>
					{week.map((day, di) => (
						<Box flex={1} aspectRatio={1} key={di}>
							{renderDay(day)}
						</Box>
					))}
				</Box>
			))}
		</Box>
	);
};
