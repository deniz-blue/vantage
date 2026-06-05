import type { Range } from "@evnt/pretty";
import type { PartialDate } from "@evnt/schema";
import { useMemo } from "react";
import { PartialDateUtil } from "@evnt/partial-date";
import { useUserLanguage, useUserTimezone } from "../../../stores/locale";
import { trynull } from "../../../utils/try";
import { Text } from "../../base/Text";

/// 2025 May 5 - 2025 May 10
export const PartialDateRangeDisplay = ({ value }: { value: Range<PartialDate> }) => {
	const language = useUserLanguage();
	const timeZone = useUserTimezone();

	const str = useMemo(() => trynull((): string => {
		let equalPrecision = PartialDateUtil.getPrecisionEquality(value.start, value.end);

		const fmt = new Intl.DateTimeFormat(language, {
			year: "numeric",
			month: "long",
			day: "numeric",
			hour: PartialDateUtil.has(value.start, "time") ? "numeric" : undefined,
			minute: PartialDateUtil.has(value.start, "time") ? "numeric" : undefined,
			hour12: false,
			timeZone,
		});

		const startTemporal = PartialDateUtil.asFormattableTemporal(value.start);
		const endTemporal = PartialDateUtil.asFormattableTemporal(value.end);
		return fmt.formatRange(startTemporal, endTemporal);
	}), [language, timeZone, value]) ?? "Error";

	return (
		<Text>
			{str}
		</Text>
	)
};
