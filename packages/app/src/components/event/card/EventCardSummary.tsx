import { ComponentType, useMemo } from "react";
import { useResolvedEvent } from "@vantage/core";
import { groupDates, formatDate, formatDateRange, formatTimeRange } from "@evnt/pretty";
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

	if (!groups || groups.length === 0) return null;

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

	const renderVenueRow = (vid: string, idx: number) => {
		const venue = venueMap.get(vid);
		if (!venue) return null;

		const icon =
			venue.$type === "directory.evnt.venue.online"
				? IconWorld
				: venue.$type === "directory.evnt.venue.physical"
					? IconMapPin
					: IconWorldPin;

		const name = TranslationsUtil.isEmpty(venue.name) ? "Unnamed venue" : translate(venue.name);

		return <SummarySnippet key={idx} icon={icon} text={name} />;
	};

	return (
		<Box mt="xs" gap={0}>
			{sharedVenues?.map(renderVenueRow)}

			{shown.map((group, i) => {
				const { dates } = group;
				if (!dates) return null;

				const date =
					dates.type === "single"
						? formatDate(dates.date, config)
						: dates.type === "range"
							? formatDateRange(dates.from, dates.to, config)
							: formatDateRange(dates.dates[0], dates.dates[dates.dates.length - 1], config);
				if (!date) return null;

				const tr = group.times[0];
				const time = tr ? formatTimeRange(tr.start, tr.end, config) : "";

				return (
					<Box key={i} gap={2}>
						<SummarySnippet icon={IconCalendar} text={date} />
						{!!time && <SummarySnippet icon={IconClock} text={time} />}
						{!sharedVenues && group.venueIds.map(renderVenueRow)}
					</Box>
				);
			})}

			{overflow > 0 && <SummarySnippet icon={IconCalendar} text={`+${overflow} more`} />}
		</Box>
	);
};

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
