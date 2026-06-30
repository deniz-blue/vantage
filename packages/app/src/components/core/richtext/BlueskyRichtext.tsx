import { useMemo } from "react";
import { segmentize, type Facet } from "@atcute/bluesky-richtext-segmenter";
import type { BlueSkyRichtextComponent } from "@evnt/types";
import { Text } from "../../base/Text";
import { FontSize } from "../../../theme/sizing";

type BskyFeature = BlueSkyRichtextComponent["facets"][number]["features"][number];

export interface BlueskyRichtextProps {
	text: string;
	facets?: BlueSkyRichtextComponent["facets"];
}

export const BlueskyRichtext = ({ text, facets }: BlueskyRichtextProps) => {
	const segments = useMemo(
		() => segmentize<BskyFeature>(text, facets as Facet<BskyFeature>[] | undefined),
		[text, facets],
	);

	return (
		<Text fz={FontSize.md}>
			{segments.map((segment, i) => {
				const hasLink = segment.features?.some(
					(f) => f.$type === "app.bsky.richtext.facet#link"
						|| f.$type === "app.bsky.richtext.facet#mention",
				);

				return hasLink
					? <Text key={i} c="Primary" tdl="underline">{segment.text}</Text>
					: segment.text;
			})}
		</Text>
	);
};
