import { formatOffset } from "@vantage/intl";
import { FontSize } from "../../../theme/sizing";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { memo, useMemo } from "react";

export const TimezoneItem = memo(({ value, selected }: { value: string; selected: boolean }) => {
	const offset = useMemo(() => formatOffset(value), [value]);

	return (
		<Box direction="row" align="center" justify="space-between" flex={1}>
			<Text fw={selected ? "bold" : undefined} numberOfLines={1}>
				{value}
			</Text>
			<Text fz={FontSize.xs} fw="bold" c={selected ? "Text" : "TextDimmed"}>
				{offset || "—"}
			</Text>
		</Box>
	);
});
