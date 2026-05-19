import { ActionIcon, Box, Button, Group, Paper, SegmentedControl, Stack, Text, Tooltip } from "@mantine/core";
import { CalendarStore, useCalendarStore } from "./useCalendarStore";
import { CalendarMonth } from "./CalendarMonth";
import { CalendarMobileMonth } from "./CalendarMobileMonth";
import { IconArrowLeft, IconArrowRight, IconChevronLeft, IconChevronRight } from "@tabler/icons-react";
import { CalendarTimeline } from "./CalendarTimeline";

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

	const monthButton = (
		<Button
			color="gray"
			size="xs"
		>
			{new Date(date).toLocaleString("default", { month: "long", year: "numeric" })}
		</Button>
	);

	return (
		<Stack gap={0} w="100%" h="100%">
			<Paper p="xs" w="100%" h="var(--app-shell-header-height)" radius={0}>
				<Group justify="space-between">
					<Group gap={0}>
						{view === "month" && (
							<Group gap={4}>
								{prevButton}
								{monthButton}
								{nextButton}
							</Group>
						)}
					</Group>
					<Group gap={4}>
						<SegmentedControl<CalendarStore["view"]>
							size="sm"
							data={[
								{ label: "Month", value: "month" },
								{ label: "Timeline", value: "timeline" },
							]}
							value={view}
							onChange={(value) => useCalendarStore.setState({ view: value })}
						/>
					</Group>
				</Group>
			</Paper>

			{view === "month" && (
				<CalendarMonth />
			)}

			{view === "timeline" && (
				<CalendarTimeline />
			)}
		</Stack>
	)
};
