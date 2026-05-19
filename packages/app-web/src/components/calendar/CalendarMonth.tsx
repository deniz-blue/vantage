import { ActionIcon, Box, Group, Paper, ScrollArea, SimpleGrid, Stack, Text, Tooltip } from "@mantine/core";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { getMonthDays } from "@mantine/dates";
import { useEffect, useState, type PropsWithChildren } from "react";
import { useCalendarStore } from "./useCalendarStore";
import { Carousel } from "@mantine/carousel";
import type { EmblaCarouselType } from "embla-carousel";
import { SmallEventsList } from "./SmallEventsList";

export const CalendarMonth = () => {
	const [embla, setEmbla] = useState<EmblaCarouselType | null>(null);

	const date = useCalendarStore((state) => state.date);
	const nextDate = Temporal.PlainDate.from(date).add({ months: 1 }).toString();
	const prevDate = Temporal.PlainDate.from(date).add({ months: -1 }).toString();

	useEffect(() => {
		if (!embla) return;
		const update = () => {
			// console.log(embla.scrollProgress());
			const progress = embla.scrollProgress();
			if (progress <= 0.0001) {
				useCalendarStore.getState().viewDelta(-1);
			} else if (progress >= 0.9999) {
				useCalendarStore.getState().viewDelta(1);
			};
		};
		embla.on("scroll", update);
		return () => {
			embla.off("scroll", update);
		};
	}, [embla]);

	useEffect(() => {
		if (!embla) return;
		embla.scrollTo(1, true);
	}, [date, embla]);

	return (
		<Carousel
			withControls={false}
			slideSize="100%"
			h="100%"
			height="100%"
			getEmblaApi={setEmbla}
		>
			<Carousel.Slide h="100%">
				<CalendarMonthContent date={prevDate} />
			</Carousel.Slide>
			<Carousel.Slide h="100%">
				<CalendarMonthContent date={date} />
			</Carousel.Slide>
			<Carousel.Slide h="100%">
				<CalendarMonthContent date={nextDate} />
			</Carousel.Slide>
		</Carousel>
	);
};

export const CalendarMonthContent = ({ date }: { date: string }) => {
	const userLanguage = useLocaleStore(store => store.language);
	const dates = getMonthDays({
		month: date,
		firstDayOfWeek: 1,
		consistentWeeks: true,
	}) as (`${number}-${number}-${number}`)[][];

	return (
		<Box w="100%" h="100%" p="xs" pt={0}>
			<Paper w="100%" h="100%" withBorder radius="md" style={{ overflow: "clip" }}>
				<Stack w="100%" h="100%" gap={0}>
					<SimpleGrid cols={7} spacing={0}>
						{[...Array(7).keys()].map((d) => (
							<Paper withBorder radius={0} key={d} style={{ borderRight: "unset", borderTop: "unset", borderBottom: "unset" }}>
								<Text ta="center" fw={500} c="dimmed">
									{new Intl.DateTimeFormat(userLanguage, { weekday: "short" }).format(new Date(2021, 0, d + 4))}
								</Text>
							</Paper>
						))}
					</SimpleGrid>

					<SimpleGrid
						cols={7}
						spacing={0}
						verticalSpacing={0}
						w="100%"
						style={{
							flex: 1,
							display: "grid",
							gridTemplateRows: "repeat(6, 1fr)",
							minHeight: 0,
						}}
					>
						{dates.map(row => (
							row.map(day => (
								<Paper
									withBorder
									key={day}
									radius={0}
									w="100%"
									style={{
										overflow: "clip",
										minHeight: 0,
										display: "flex",
										borderRight: "unset",
										borderBottom: "unset",
									}}
									pos="relative"
								>
									<CalendarMonthDay
										day={day}
										isCurrentMonth={day.slice(0, 7) === date.slice(0, 7)}
									>
										<SmallEventsList
											day={day}
										/>
									</CalendarMonthDay>
								</Paper>
							))
						))}
					</SimpleGrid>
				</Stack>
			</Paper>
		</Box>
	)
};

export const CalendarMonthDay = ({
	day,
	children,
	isCurrentMonth,
}: PropsWithChildren<{
	day: `${number}-${number}-${number}`;
	isCurrentMonth: boolean;
}>) => {
	const isToday = Temporal.Now.plainDateISO().toString() === day;

	return (
		<Stack gap={0} w="100%" h="100%">
			<Text
				c={isCurrentMonth ? (isToday ? "blue" : undefined) : "dimmed"}
				fz="sm"
				p={4}
				fw="bold"
				span
			>
				{day.slice(-2)}
			</Text>
			<ScrollArea h="100%" mah="100%" w="100%" scrollbars="y">
				{children}
			</ScrollArea>
		</Stack>
	);
};

