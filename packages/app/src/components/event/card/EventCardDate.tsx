import { useMemo } from "react";
import { useResolvedEvent } from "@vantage/core";
import { groupDates } from "@evnt/pretty";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { Sizing } from "../../../theme/sizing";
import {
	formatDateLabel,
	formatTimeRange,
	formatDateRange,
} from "./format-partial-date";

const MAX_VISIBLE = 2;

export const EventCardDate = () => {
	const { data } = useResolvedEvent();
	const locale = useLocaleStore((s) => s.language);

	const groups = useMemo(() => {
		if (!data?.instances) return null;
		return groupDates(data.instances, true);
	}, [data?.instances]);

	if (!groups || groups.length === 0) return null;

	const shown = groups.slice(0, MAX_VISIBLE);
	const overflow = groups.length - MAX_VISIBLE;

	return (
		<Box mt={6} gap={2}>
			{shown.map((group, i) => {
				const first = group.entries[0];
				const last = group.entries[group.entries.length - 1];
				const tr = group.timeRanges[0];

				// Build date portion
				const date = last && last !== first
					? formatDateRange(first?.start, last?.start, locale)
					: formatDateLabel(first?.start ?? first?.end ?? "", locale);
				if (!date) return null;

				// Build time portion
				const time = formatTimeRange(tr?.start, tr?.end, locale);

				return (
					<Text key={i} fz={Sizing.fontSizeSm} c="TextDimmed" numberOfLines={1}>
						{date}
						{time && <Text fz={Sizing.fontSizeSm} c="TextDimmed">{` · ${time}`}</Text>}
					</Text>
				);
			})}

			{overflow > 0 && (
				<Text fz={Sizing.fontSizeSm} c="TextDimmed" style={{ fontStyle: "italic" }}>
					+{overflow} more
				</Text>
			)}
		</Box>
	);
};
