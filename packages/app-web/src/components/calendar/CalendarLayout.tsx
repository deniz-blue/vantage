import { ActionIcon, Box, Button, Group, Paper, Stack, Text, Tooltip } from "@mantine/core";
import { useCalendarStore } from "./useCalendarStore";
import { CalendarMonth } from "./CalendarMonth";
import { CalendarMobileMonth } from "./CalendarMobileMonth";
import { IconArrowLeft, IconArrowRight, IconChevronLeft, IconChevronRight } from "@tabler/icons-react";

export const CalendarLayout = () => {
	const view = useCalendarStore((state) => state.view);
	const date = useCalendarStore((state) => state.date);

	const [prevButton, nextButton] = [-1, +1].map(dir => (
		<Tooltip label={dir === -1 ? "Previous month" : "Next month"}>
			<ActionIcon
				size="input-xs"
				color="gray"
				onClick={() => useCalendarStore.getState().viewDelta(dir as 1 | -1)}
			>
				{dir === -1 ? <IconChevronLeft /> : <IconChevronRight />}
			</ActionIcon>
		</Tooltip>
	));

	return (
		<Stack gap={0} w="100%" h="100%">
			<Paper p="xs" w="100%" radius={0}>
				<Group justify="space-between">
					<Group gap={4}>
						{prevButton}
						<Button
							color="gray"
							size="xs"
						>
							{new Date(date).toLocaleString("default", { month: "long", year: "numeric" })}
						</Button>
						{nextButton}
					</Group>
					<Group gap={4}>

					</Group>
				</Group>
			</Paper>

			{view === "month" && (
				<CalendarMonth />
			)}
			{view === "month-mobile" && (
				<CalendarMobileMonth />
			)}
		</Stack>
	)
};
