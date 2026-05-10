import { Stack } from "@mantine/core";
import { SmallTitle } from "../../base/SmallTitle";
import { EventLinkButton } from "../link/EventLinkButton";
import type { LinkComponent } from "@evnt/schema";
import { useResolvedEvent } from "../../../../db/resolved-event";

export const EventDetailsLinks = () => {
	const { data } = useResolvedEvent();

	const links = data?.components
		?.filter((component): component is LinkComponent => component.$type === "directory.evnt.component.link");

	if (!links || links.length === 0) return null;

	return (
		<Stack gap={0} component="section">
			<SmallTitle>
				links
			</SmallTitle>
			<Stack gap={4}>
				{links.map((link, index) => (
					<EventLinkButton key={index} value={link} />
				))}
			</Stack>
		</Stack>
	)
};
