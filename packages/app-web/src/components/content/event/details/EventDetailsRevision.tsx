import { Stack } from "@mantine/core";
import { useResolvedEvent } from "@vantage/core";
import { SmallTitle } from "../../base/SmallTitle";

export const EventDetailsRevision = () => {
	const { revision } = useResolvedEvent();

	return (
		<Stack gap="0" component="section">
			<SmallTitle>
				revision
			</SmallTitle>
		</Stack>
	)
};
