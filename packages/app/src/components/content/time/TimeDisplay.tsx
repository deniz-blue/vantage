import { PartialDate, PartialDateUtil } from "@evnt/partial-date";
import { useMemo } from "react";
import { useUserTimezone } from "../../../stores/locale";
import { trynull } from "../../../utils/try";
import { Text } from "../../base/Text";

/// Displays a time (clock part) i.e 15:00
export const TimeDisplay = ({
	value,
}: {
	value: PartialDate.YearMonthDayTime;
}) => {
	const userTimezone = useUserTimezone();

	const parsed = PartialDateUtil.parse(value);

	const sameTimezone = parsed.timezone === userTimezone;

	const time = useMemo(() => trynull(() => PartialDateUtil.asPlainDateTime(parsed).toLocaleString(undefined, {
		hour: "2-digit",
		minute: "2-digit",
		hour12: false,
	})), [value]) ?? "!";

	const localizedTime = useMemo(() => trynull(() => PartialDateUtil.asZonedDateTime(parsed).toInstant().toLocaleString(undefined, {
		hour: "2-digit",
		minute: "2-digit",
		hour12: false,
		timeZone: userTimezone,
	})), [value, userTimezone]) ?? "!";

	return (
		<Text>
			{time ?? "!"}{!sameTimezone && (time !== localizedTime) && <Text c="TextDimmed" children={` (${localizedTime})`} />}
		</Text>
	);
};
