import { PartialDate } from "@evnt/types";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { useMemo } from "react";
import { PartialDateUtil } from "@evnt/partial-date";
import { Text, TextProps } from "../base/Text";

export const PartialDateLabel = ({ value, ...props }: Omit<TextProps, "children"> & { value: PartialDate }) => {
	const language = useLocaleStore((s) => s.language);

	const label = useMemo(() => {
		const parsed = PartialDateUtil.parse(value);
		const currentYear = new Date().getFullYear();
		const fmt = new Intl.DateTimeFormat(language, {
			year: parsed.year !== currentYear ? "numeric" : undefined,
			month: PartialDateUtil.has(parsed, "month") ? "long" : undefined,
			day: PartialDateUtil.has(parsed, "day") ? "numeric" : undefined,
			hour: PartialDateUtil.has(parsed, "time") ? "numeric" : undefined,
			minute: PartialDateUtil.has(parsed, "time") ? "numeric" : undefined,
			calendar: "iso8601",
			hour12: false,
			timeZone: parsed.timezone,
		});
		const temporal = PartialDateUtil.asFormattableTemporal(parsed);
		return fmt.format(temporal);
	}, [value, language]);

	return (
		<Text
			children={label}
			{...props}
		/>
	);
};
