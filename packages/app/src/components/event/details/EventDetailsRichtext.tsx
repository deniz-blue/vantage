import { useResolvedEvent } from "@vantage/core";
import type { MarkdownComponent, BlueSkyRichtextComponent } from "@evnt/types";
import { Box } from "../../base/Box";
import { SmallTitle } from "./SmallTitle";
import { Spacing } from "../../../theme/spacing";
import { MarkdownRichtext } from "../../core/richtext/MarkdownRichtext";
import { BlueskyRichtext } from "../../core/richtext/BlueskyRichtext";

export const EventDetailsRichtext = () => {
	const { data } = useResolvedEvent();

	const richtexts = data?.components?.filter(
		(c) =>
			c.$type === "directory.evnt.component.markdown"
			|| c.$type === "directory.evnt.component.blueSkyRichtext",
	);

	if (!richtexts || richtexts.length === 0) return null;

	return (
		<Box gap={Spacing.sm}>
			{richtexts.map((comp, i) => {
				switch (comp.$type) {
					case "directory.evnt.component.markdown": {
						const md = comp as MarkdownComponent;
						return (
							<Box key={i} gap={4}>
								<SmallTitle>Description</SmallTitle>
								<MarkdownRichtext content={md.content} />
							</Box>
						);
					}
					case "directory.evnt.component.blueSkyRichtext": {
						const bsky = comp as BlueSkyRichtextComponent;
						return (
							<Box key={i} gap={4}>
								<SmallTitle>Description</SmallTitle>
								<BlueskyRichtext text={bsky.text} facets={bsky.facets} />
							</Box>
						);
					}
				}
			})}
		</Box>
	);
};
