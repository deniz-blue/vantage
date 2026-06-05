import type { PartialDate } from "@evnt/schema";
import { PartialDateUtil } from "@evnt/partial-date";
import { useMemo } from "react";
import { useUserLanguage, useUserTimezone } from "../../../stores/locale";
import { trynull } from "../../../utils/try";
import { Text } from "../../base/Text";

export const PartialDateDisplay = ({
	value,
}: {
	value: PartialDate | undefined;
}) => {
	const userLanguage = useUserLanguage();
	const userTimezone = useUserTimezone();

	const str = useMemo(() => trynull(() => {
		if (!value) return "";

		const parsed = PartialDateUtil.parse(value);

		const currentYear = new Date().getFullYear();

		const fmt = new Intl.DateTimeFormat(userLanguage, {
			year: parsed.year !== currentYear ? "numeric" : undefined,
			month: PartialDateUtil.has(parsed, "month") ? "long" : undefined,
			day: PartialDateUtil.has(parsed, "day") ? "numeric" : undefined,
			hour: PartialDateUtil.has(parsed, "time") ? "numeric" : undefined,
			minute: PartialDateUtil.has(parsed, "time") ? "numeric" : undefined,
			weekday: PartialDateUtil.has(parsed, "day") ? "long" : undefined,
			calendar: "iso8601",
			hour12: false,
			timeZone: parsed.timezone,
		});

		let temporal = PartialDateUtil.asFormattableTemporal(parsed);
		let str = fmt.format(temporal);

		if (parsed.precision === "time" && parsed.timezone !== userTimezone) {
			const localizedFmt = new Intl.DateTimeFormat(userLanguage, {
				hour: "numeric",
				minute: "numeric",
				hour12: false,
				timeZone: userTimezone,
			});
			const localizedParts = localizedFmt.format(PartialDateUtil.asZonedDateTime(parsed).toInstant());
			str += ` (${localizedParts})`;
		}

		return str;
	}), [value, userLanguage, userTimezone]) ?? "Error";

	return (
		<Text c={str ? undefined : "TextDimmed"}>
			{str || "<unknown>"}
		</Text>
	)
};
