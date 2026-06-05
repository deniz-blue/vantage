import type { SnippetLabelProps } from "@evnt/pretty";
import { Box } from "../../base/Box";
import { TimeDisplay } from "./TimeDisplay";
import { Text } from "../../base/Text";

export const TimeRangeDisplay = ({ value }: SnippetLabelProps<"time-range">) => {
	return (
		<Box
			direction="row"
		>
			<TimeDisplay
				value={value.start}
			/>
			<Text
				c="TextDimmed"
				children=" – "
			/>
			<TimeDisplay
				value={value.end}
			/>
		</Box>
	)
};
