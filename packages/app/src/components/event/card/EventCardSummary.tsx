import { Fragment, useCallback, useMemo } from "react";
import { useResolvedEvent } from "@vantage/core";
import { groupDates, formatDate, formatTimeRange } from "@evnt/pretty";
import type { Venue } from "@evnt/types";
import { TranslationsUtil } from "@evnt/translations";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { useTranslator } from "../../../hooks/useTranslator";
import {
	IconCalendar,
	IconClock,
	IconMapPin,
	IconWorld,
	IconWorldPin,
} from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { FontSize, IconSize } from "../../../theme/sizing";
import { useMappingHelper } from "@shopify/flash-list";

const MAX_VISIBLE = 3;

export const EventCardSummary = () => {
	const { data } = useResolvedEvent();
	const { getMappingKey } = useMappingHelper();
	const locale = useLocaleStore((s) => s.language);
	const timezone = useLocaleStore((s) => s.timezone);
	const translate = useTranslator();

	const groups = useMemo(() => {
		if (!data?.instances) return [];
		return groupDates(data.instances, true);
	}, [data?.instances]);

	const venueMap = useMemo(() => {
		if (!data?.venues) return new Map<string, Venue>();
		return new Map(data.venues.map((v) => [v.id, v]));
	}, [data?.venues]);

	const getVenueSnippetProps = useCallback(
		(venue: Venue): SummarySnippetProps => {
			return {
				icon:
					venue.$type === "directory.evnt.venue.online"
						? "world"
						: venue.$type === "directory.evnt.venue.physical"
							? "map-pin"
							: "world-pin",
				text: TranslationsUtil.isEmpty(venue.name) ? "Unnamed venue" : translate(venue.name),
			};
		},
		[translate],
	);

	const children = useMemo(() => {
		const children: SummarySnippetProps[] = [];

		const shown = groups.slice(0, MAX_VISIBLE);
		const overflow = groups.length - MAX_VISIBLE;
		const config = { language: locale, timezone, compactDates: true };

		// If all shown groups share the same venueIds, render venues once at top

		const sharedVenues = shown
			.filter(
				(g) =>
					JSON.stringify(Array.from(g.venueIds ?? []).sort()) ==
					JSON.stringify(Array.from(new Set(data?.venues?.map((v) => v.id) ?? [])).sort()),
			)
			.map((g) => g.venueIds)
			.flat();

		for (const venueId of sharedVenues ?? []) {
			const venue = venueMap.get(venueId);
			if (!venue) continue;
			children.push(getVenueSnippetProps(venue));
		}

		for (const { dates, times, venueIds } of shown) {
			if (dates.type === "single") {
				children.push({
					icon: "calendar",
					text: formatDate(dates.date, config) ?? "",
				});
			} else if (dates.type === "range") {
				// TODO(upstream): Fix formatting of date ranges in @evnt/pretty
				// children.push({
				// 	icon: IconCalendar,
				// 	text: formatDateRange(dates.from, dates.to, config) ?? "",
				// });
			} else if (dates.type === "list") {
				for (const date of dates.dates) {
					children.push({
						icon: "calendar",
						text: formatDate(date, config) ?? "",
					});
				}
			}

			for (const time of times) {
				children.push({
					icon: "clock",
					text: formatTimeRange(time.start, time.end, config) ?? "",
				});
			}

			for (const venueId of venueIds.filter((id) => !sharedVenues?.includes(id))) {
				const venue = venueMap.get(venueId);
				if (!venue) continue;
				children.push(getVenueSnippetProps(venue));
			}
		}

		if (overflow > 0)
			children.push({
				icon: "calendar",
				text: `+${overflow} more`,
			});

		return children;
	}, [groups, venueMap, getVenueSnippetProps, locale, timezone]);

	return (
		<Box mt="xs" gap={0}>
			{children.map((child, index) => (
				<SummarySnippet key={getMappingKey(index, index)} {...child} />
			))}
		</Box>
	);
};

export interface SummarySnippetProps {
	icon: "calendar" | "clock" | "map-pin" | "world" | "world-pin";
	text: string;
}

export const SummarySnippet = ({ icon, text }: SummarySnippetProps) => {
	const Icon = useMemo(() => {
		if (icon === "calendar") return IconCalendar;
		if (icon === "clock") return IconClock;
		if (icon === "map-pin") return IconMapPin;
		if (icon === "world") return IconWorld;
		if (icon === "world-pin") return IconWorldPin;
		return Fragment;
	}, [icon]);

	return (
		<Box direction="row" align="center" gap={0}>
			<Icon size={IconSize.sm} color={Colors.Text} />
			<Box flex={1}>
				<Text fz={FontSize.sm} numberOfLines={1}>
					{text}
				</Text>
			</Box>
		</Box>
	);
};
