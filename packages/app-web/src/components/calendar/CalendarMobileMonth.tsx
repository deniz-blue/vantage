import { Box, ScrollArea, Stack } from "@mantine/core";
import { DatePicker, MonthLevel, MonthPicker } from "@mantine/dates";
import { useCalendarStore } from "./useCalendarStore";

export interface CalendarMobileMonthProps {
	month: `${number}-${number}`;
	setMonth: (month: `${number}-${number}`) => void;
	day: `${number}-${number}-${number}`;
	setDay: (day: `${number}-${number}-${number}`) => void;
	renderDay: (props: { day: `${number}-${number}-${number}` }) => React.ReactNode;
	renderDayButton?: (props: { day: `${number}-${number}-${number}` }) => React.ReactNode;
}

export const CalendarMobileMonth = () => {
	const date = useCalendarStore((state) => state.date);

	return (
		<Stack w="100%" h="100%" gap={0} align="center">
			<DatePicker
				fullWidth
				date={date}
				value={date}
				onDateChange={(date) => useCalendarStore.setState({ date: date as `${number}-${number}-${number}` })}
				onChange={(date) => useCalendarStore.setState({ date: date as `${number}-${number}-${number}` })}
				renderDay={undefined}
				level="month"
			/>
			<ScrollArea w="100%">
				<Stack w="100%" h="100%" gap={0}>
					<Box p={4} pb="xl">
						meow
					</Box>
				</Stack>
			</ScrollArea>
		</Stack>
	);
};
