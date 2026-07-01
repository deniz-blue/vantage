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
			c.$type === "directory.evnt.richtext.markdown"
			|| c.$type === "directory.evnt.richtext.bsky",
	);

	if (!richtexts || richtexts.length === 0) return null;

	return (
		<Box gap={Spacing.sm}>
			{richtexts.map((comp, i) => {
				switch (comp.$type) {
					case "directory.evnt.richtext.markdown": {
						const md = comp as MarkdownComponent;
						console.log(md)
						return (
							<Box key={i} gap={4}>
								<SmallTitle>Description</SmallTitle>
								<MarkdownRichtext content={md.content} />
							</Box>
						);
					}
					case "directory.evnt.richtext.bsky": {
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
