import { useMemo } from "react";
import { segmentize } from "@atcute/bluesky-richtext-segmenter";
import type { RichTextBlueskyComponent } from "@evnt/types";
import { Text } from "../../base/Text";
import { FontSize } from "../../../theme/sizing";

export interface BlueskyRichtextProps {
	content: Omit<RichTextBlueskyComponent, "$type">;
}

export const BlueskyRichtext = ({ content: { text, facets } }: BlueskyRichtextProps) => {
	const segments = useMemo(() => segmentize(text, facets), [text, facets]);

	return (
		<Text fz={FontSize.md}>
			{segments.map((segment, i) => {
				const hasLink = segment.features?.some(
					(f) =>
						f.$type === "app.bsky.richtext.facet#link" ||
						f.$type === "app.bsky.richtext.facet#mention",
				);

				return hasLink ? (
					<Text key={i} c="Primary" tdl="underline">
						{segment.text}
					</Text>
				) : (
					segment.text
				);
			})}
		</Text>
	);
};
