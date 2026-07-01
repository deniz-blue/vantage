import { useCallback, useMemo, useState } from "react";
import {
	ActivityIndicator,
} from "react-native";
import { IconChevronLeft, IconChevronRight } from "@tabler/icons-react-native";
import { useEventListQuery, ResolvedEventContext } from "@vantage/core";
import { PartialDateUtil } from "@evnt/partial-date";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Button } from "../../components/base/button/Button";
import { ActionIcon } from "../../components/base/button/ActionIcon";
import { CalendarMonth } from "../../components/core/calendar-month/CalendarMonth";
import type { CalendarDay } from "../../components/core/calendar-month/calendar-month-utils";
import { Colors } from "../../theme/colors";
import { FontSize, IconSize } from "../../theme/sizing";
import { Sheet } from "../../components/base/Sheet";
import { EventCard } from "../../components/event/card/EventCard";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { EmptyState } from "../../components/base/EmptyState";
import { Container } from "../../components/base/Container";

function useEventsByDay(events: { data: Vantage.ResolvedEvent | null | undefined }[]) {
	return useMemo(() => {
		const map = new Map<string, number>();
		for (const event of events) {
			const resolved = event.data;
			if (!resolved?.data?.instances) continue;
			for (const instance of resolved.data.instances) {
				if (!instance.start) continue;
				if (!PartialDateUtil.has(instance.start, "day")) continue;
				const parsed = PartialDateUtil.parse(instance.start);
				if ("day" in parsed) {
					const day = `${parsed.year}-${String(parsed.month).padStart(2, "0")}-${String(parsed.day).padStart(2, "0")}`;
					map.set(day, (map.get(day) ?? 0) + 1);

					const low = PartialDateUtil.toInstant(instance.start, "low");
					const high = instance.end
						? PartialDateUtil.toInstant(instance.end, "high")
						: PartialDateUtil.toInstant(instance.start, "high");
					console.log(day, instance, { low: low.epochMilliseconds, high: high.epochMilliseconds })
				}
			}
		}
		return map;
	}, [events]);
}

export default function CalendarPage() {
	const userLanguage = useLocaleStore((s) => s.language);
	const userTimezone = useLocaleStore((s) => s.timezone);
	const [currentDate, setCurrentDate] = useState(Temporal.Now.plainDateISO());
	const [selectedDay, setSelectedDay] = useState<string | null>(null);

	const monthLabel = useMemo(() => {
		const fmt = new Intl.DateTimeFormat(userLanguage, { month: "long", year: "numeric" });
		const d = new Temporal.PlainDate(currentDate.year, currentDate.month, 1);
		return fmt.format(d);
	}, [currentDate, userLanguage]);

	const monthStart = useMemo(() => {
		return currentDate.toZonedDateTime({ timeZone: userTimezone }).toInstant().epochMilliseconds;
	}, [currentDate]);

	const monthEnd = useMemo(() => {
		return currentDate.add({ months: 1 }).toZonedDateTime({ timeZone: userTimezone }).toInstant().epochMilliseconds - 1;
	}, [currentDate]);

	const { events } = useEventListQuery({
		afterTimestamp: monthStart,
		beforeTimestamp: monthEnd,
		limit: 500,
	});

	const eventsByDay = useEventsByDay(events);

	// Navigate months
	const goToPrevMonth = useCallback(() => {
		setCurrentDate((d) => d.add({ months: -1 }));
	}, []);
	const goToNextMonth = useCallback(() => {
		setCurrentDate((d) => d.add({ months: 1 }));
	}, []);
	const goToToday = useCallback(() => {
		setCurrentDate(Temporal.Now.plainDateISO());
	}, []);

	const handleDayPress = useCallback((day: CalendarDay) => {
		const dayStr = `${day.year}-${String(day.month).padStart(2, "0")}-${String(day.day).padStart(2, "0")}`;
		setSelectedDay(dayStr);
	}, []);

	const renderDay = useCallback(
		(day: CalendarDay) => {
			const dayStr = `${day.year}-${String(day.month).padStart(2, "0")}-${String(day.day).padStart(2, "0")}`;
			const eventCount = eventsByDay.get(dayStr) ?? 0;

			return (
				<ActionIcon
					onPress={() => handleDayPress(day)}
					w="100%"
					h="100%"
					style={{ aspectRatio: 1 }}
					variant={day.isToday ? "light" : "subtle"}
				>
					<Box
						flex={1}
						aspectRatio={1}
						align="center"
						justify="center"
						gap={2}
					>
						<Text
							fz={FontSize.sm}
							c={(
								day.isToday
									? "White"
									: day.isOutsideMonth
										? "TextDimmed"
										: "Text"
							)}
						>
							{day.day}
						</Text>

						<Box direction="row" gap={2}>
							{new Array(Math.min(eventCount, 5)).fill(0).map((_, i) => (
								<Box
									key={i}
									w={5}
									h={5}
									bg={Colors.Primary}
									radius={999}
								/>
							))}
						</Box>
					</Box>
				</ActionIcon>
			);
		},
		[eventsByDay, handleDayPress],
	);

	return (
		<Container flex={1}>
			{/* Header */}
			<Box
				direction="row"
				align="center"
				justify="space-between"
				gap="xs"
				p="md"
			>
				<Box direction="row" align="center" gap="xs" flex={1}>
					<ActionIcon onPress={goToPrevMonth} size="sm">
						<IconChevronLeft size={IconSize.xs} />
					</ActionIcon>
					<Button size="sm" flex={1}>
						<Box align="center" flex={1}>
							<Text fw="bold">
								{monthLabel}
							</Text>
						</Box>
					</Button>
					<ActionIcon onPress={goToNextMonth} size="sm">
						<IconChevronRight size={IconSize.xs} />
					</ActionIcon>
				</Box>
				<Button onPress={goToToday} size="sm">
					Today
				</Button>
			</Box>

			<Box flex={1} p="xs">
				<CalendarMonth
					year={currentDate.year}
					month={currentDate.month}
					renderDay={renderDay}
					gap="xs"
				/>
			</Box>

			{/* Day events sheet */}
			<Sheet open={!!selectedDay} onClose={() => setSelectedDay(null)} scrollable>
				{selectedDay && (
					<DayEventsContent day={selectedDay} />
				)}
			</Sheet>
		</Container>
	);
}

/** Fetches and renders events for a specific day inside a sheet */
const DayEventsContent = ({ day }: { day: string }) => {
	const userTimezone = useLocaleStore(s => s.timezone);

	const lowTimestamp = Temporal.PlainDate.from(day)
		.add({ days: -1 })
		.toZonedDateTime({ timeZone: "UTC" })
		.toInstant()
		.epochMilliseconds;

	const highTimestamp = Temporal.PlainDate.from(day)
		.add({ days: 1 })
		.toZonedDateTime({ timeZone: "UTC" })
		.toInstant()
		.epochMilliseconds;

	const { events, rowsQuery } = useEventListQuery({
		beforeTimestamp: highTimestamp,
		afterTimestamp: lowTimestamp,
		limit: 100,
	});

	const locale = useLocaleStore((s) => s.language);
	const dateLabel = useMemo(() => {
		const d = Temporal.PlainDate.from(day);
		const fmt = new Intl.DateTimeFormat(locale, {
			weekday: "long",
			month: "long",
			day: "numeric",
		});
		return fmt.format(d);
	}, [day, locale]);

	return (
		<Box p="md" gap="md">
			<Box direction="row" justify="space-between">
				<Text fz={FontSize.lg} fw="bold">
					{dateLabel}
				</Text>

				{(rowsQuery.isFetching || events.some(q => q.isFetching)) && (
					<ActivityIndicator />
				)}
			</Box>
			{events.length === 0 ? (
				<EmptyState message="No events on this day" />
			) : (
				<Box gap="sm">
					{events.map((event, index) => (
						<ResolvedEventContext.Provider
							key={event.data?.id ?? index}
							value={event.data ?? null}
						>
							<EventCard />
						</ResolvedEventContext.Provider>
					))}
				</Box>
			)}
		</Box>
	);
};
