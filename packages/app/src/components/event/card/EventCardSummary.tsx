import { ComponentType, useMemo } from "react";
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
	IconProps,
	IconWorld,
	IconWorldPin,
} from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { FontSize, IconSize } from "../../../theme/sizing";

const MAX_VISIBLE = 3;

export const EventCardSummary = () => {
	const { data } = useResolvedEvent();
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

	const shown = groups.slice(0, MAX_VISIBLE);
	const overflow = groups.length - MAX_VISIBLE;
	const config = { language: locale, timezone, compactDates: true };

	// If all shown groups share the same venueIds, render venues once at top
	const allShareVenues =
		shown.length > 1
			? shown.every(
					(g) =>
						g.venueIds.length === shown[0].venueIds.length &&
						g.venueIds.every((id, i) => id === shown[0].venueIds[i]),
				)
			: false;
	const sharedVenues = allShareVenues ? shown[0].venueIds : null;

	const getVenueSnippetProps = (venue: Venue): SummarySnippetProps => {
		return {
			icon:
				venue.$type === "directory.evnt.venue.online"
					? IconWorld
					: venue.$type === "directory.evnt.venue.physical"
						? IconMapPin
						: IconWorldPin,
			text: TranslationsUtil.isEmpty(venue.name) ? "Unnamed venue" : translate(venue.name),
		};
	};

	const children = useMemo(() => {
		const children: SummarySnippetProps[] = [];

		for (const venueId of sharedVenues ?? []) {
			const venue = venueMap.get(venueId);
			if (!venue) continue;
			children.push(getVenueSnippetProps(venue));
		}

		for (const { dates, times, venueIds } of shown) {
			if (dates.type === "single") {
				children.push({
					icon: IconCalendar,
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
						icon: IconCalendar,
						text: formatDate(date, config) ?? "",
					});
				}
			}

			for (const time of times) {
				children.push({
					icon: IconClock,
					text: formatTimeRange(time.start, time.end, config) ?? "",
				});
			}

			for (const venueId of venueIds) {
				const venue = venueMap.get(venueId);
				if (!venue) continue;
				children.push(getVenueSnippetProps(venue));
			}
		}

		if (overflow > 0)
			children.push({
				icon: IconCalendar,
				text: `+${overflow} more`,
			});

		return children;
	}, []);

	return (
		<Box mt="xs" gap={0}>
			{children.map((child, index) => (
				<SummarySnippet key={index} icon={child.icon} text={child.text} />
			))}
		</Box>
	);
};

export interface SummarySnippetProps {
	icon: ComponentType<IconProps>;
	text: string;
}

export const SummarySnippet = ({
	icon: Icon,
	text,
}: {
	icon: ComponentType<IconProps>;
	text: string;
}) => {
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
