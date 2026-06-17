import { useMemo, type ReactNode } from "react";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { FontSize, Radius } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";
import { getCalendarGrid, type CalendarDay } from "./calendar-month-utils";
import { useLocaleStore } from "../../../stores/useLocaleStore";

export interface CalendarFullYearProps {
	year: number;
	firstDayOfWeek?: 0 | 1;
	/** Number of month columns. Default 4. */
	columns?: number;
	renderDay?: (day: CalendarDay) => ReactNode;
	renderMonthHeader?: (month: number, monthName: string) => ReactNode;
}

const defaultRenderMonthHeader = (_month: number, name: string) => (
	<Box mb="xs">
		<Text fz={FontSize.sm} fw="600" ta="center" c="Text">
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
		<Text
			fz={10}
			c={day.isToday ? "White" : day.isOutsideMonth ? "TextDimmed" : "Text"}
		>
			{day.day}
		</Text>
	</Box>
);

export const CalendarFullYear = ({
	year,
	firstDayOfWeek = 1,
	columns = 4,
	renderDay = defaultRenderDay,
	renderMonthHeader,
}: CalendarFullYearProps) => {
	const locale = useLocaleStore((s) => s.language);

	const monthNames = useMemo(() => {
		const fmt = new Intl.DateTimeFormat(locale, { month: "short" });
		return Array.from({ length: 12 }, (_, i) => {
			const date = new Temporal.PlainDate(year, i + 1, 1);
			return fmt.format(date);
		});
	}, [locale, year]);

	const gridCache = useMemo(() => {
		const grids: { name: string; weeks: CalendarDay[][] }[] = [];
		for (let m = 0; m < 12; m++) {
			const month = m + 1;
			const grid = getCalendarGrid(year, month, firstDayOfWeek);
			const weeks: CalendarDay[][] = [];
			for (let i = 0; i < grid.days.length; i += 7) {
				weeks.push(grid.days.slice(i, i + 7));
			}
			grids.push({ name: monthNames[m]!, weeks });
		}
		return grids;
	}, [year, firstDayOfWeek, monthNames]);

	const renderHeader = renderMonthHeader ?? defaultRenderMonthHeader;

	const rows: { name: string; weeks: CalendarDay[][] }[][] = [];
	for (let i = 0; i < gridCache.length; i += columns) {
		rows.push(gridCache.slice(i, i + columns));
	}

	return (
		<Box gap="md">
			{rows.map((row, ri) => (
				<Box direction="row" gap="md" key={ri}>
					{row.map((monthData, mi) => (
						<Box flex={1} key={mi}>
							{renderHeader(ri * columns + mi + 1, monthData.name)}

							{monthData.weeks.map((week, wi) => (
								<Box direction="row" key={wi}>
									{week.map((day, di) => (
										<Box flex={1} key={di}>
											{renderDay(day)}
										</Box>
									))}
								</Box>
							))}
						</Box>
					))}
				</Box>
			))}
		</Box>
	);
};
