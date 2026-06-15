import { useMemo } from "react";
import { useResolvedEvent } from "@vantage/core";
import { groupDates, formatDate, formatDateRange, formatTimeRange } from "@evnt/pretty";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { IconCalendar, IconClock } from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { Sizing } from "../../../theme/sizing";


const MAX_VISIBLE = 3;

export const EventCardSummary = () => {
	const { data } = useResolvedEvent();
	const locale = useLocaleStore((s) => s.language);
	const timezone = useLocaleStore((s) => s.timezone);

	const groups = useMemo(() => {
		if (!data?.instances) return [];
		return groupDates(data.instances, true);
	}, [data?.instances]);

	if (!groups || groups.length === 0) return null;

	const shown = groups.slice(0, MAX_VISIBLE);
	const overflow = groups.length - MAX_VISIBLE;
	const config = { language: locale, timezone, compactDates: true };

	return (
		<Box mt="xs" gap={4}>
			{shown.map((group, i) => {
				const { dates } = group;
				if (!dates) return null;

				const date = dates.type === "single"
					? formatDate(dates.date, config)
					: dates.type === "range"
						? formatDateRange(dates.from, dates.to, config)
						: formatDateRange(
							dates.dates[0],
							dates.dates[dates.dates.length - 1],
							config,
						);
				if (!date) return null;

				const tr = group.times[0];
				const time = tr ? formatTimeRange(tr.start, tr.end, config) : "";

				return (
					<Box key={i} gap={2}>
						<Box direction="row" gap={4}>
							<IconCalendar size={Sizing.iconMd} color={Colors.Text} />
							<Text fz={Sizing.fontSizeMd} numberOfLines={1}>
								{date}
							</Text>
						</Box>
						{!!time && (
							<Box direction="row" gap={4}>
								<IconClock size={Sizing.iconMd} color={Colors.Text} />
								<Text fz={Sizing.fontSizeMd} numberOfLines={1}>
									{time}
								</Text>
							</Box>
						)}
					</Box>
				);
			})}

			{(overflow > 0) && (
				<Box direction="row" gap={4}>
					<IconCalendar size={Sizing.iconMd} color={Colors.Text} />
					<Text fz={Sizing.fontSizeMd} fst="italic">
						+{overflow} more
					</Text>
				</Box>
			)}
		</Box>
	);
};
