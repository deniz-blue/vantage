import { useResolvedEvent } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { SmallTitle } from "./SmallTitle";
import { Spacing } from "../../../theme/spacing";

type RichtextComponent = Record<string, unknown> & { $type: string };

const richtextRenderers = {
	"directory.evnt.richtext.markdown": MarkdownRenderer,
	"app.bsky.richtext": BskyRichtextRenderer,
} as Record<string, (comp: RichtextComponent) => React.ReactNode | null>;

export const EventDetailsRichtext = () => {
	const { data } = useResolvedEvent();

	const richtexts = data?.components?.filter(
		(c) => c.$type in richtextRenderers,
	);

	if (!richtexts || richtexts.length === 0) return null;

	return (
		<Box gap={Spacing.sm}>
			{richtexts.map((comp, i) => {
				const renderer = richtextRenderers[comp.$type];
				if (!renderer) return null;
				const rendered = renderer(comp as RichtextComponent);
				if (!rendered) return null;

				return (
					<Box key={i} gap={4}>
						<SmallTitle>Description</SmallTitle>
						{rendered}
					</Box>
				);
			})}
		</Box>
	);
};

function MarkdownRenderer(comp: RichtextComponent) {
	const content = comp.markdown as string | undefined;
	if (!content || typeof content !== "string") return null;
	return <Text fz={15}>{content}</Text>;
}

function BskyRichtextRenderer(comp: RichtextComponent) {
	const text = comp.text as string | undefined;
	const facets = comp.facets as Array<{
		$type: string;
		index: { byteStart: number; byteEnd: number };
		features: Array<{ $type: string; uri?: string; did?: string }>;
	}> | undefined;

	if (!text || typeof text !== "string") return null;

	const segmentize = requireSegmentize();
	if (!segmentize) {
		return <Text fz={15}>{text}</Text>;
	}

	const segments = segmentize(text, facets ?? []);

	return (
		<Text fz={15}>
			{segments.map((segment: any, i: number) => {
				let content: React.ReactNode = segment.text;

				if (segment.features) {
					for (const feature of segment.features) {
						if (feature.$type === "app.bsky.richtext.facet#link" && feature.uri) {
							content = (
								<Text key={i} fz={15} c="Primary" tdl="underline">
									{content}
								</Text>
							);
						} else if (feature.$type === "app.bsky.richtext.facet#mention" && feature.did) {
							content = (
								<Text key={i} fz={15} c="Primary" tdl="underline">
									{content}
								</Text>
							);
						}
					}
				}

				return <Text key={i} fz={15}>{content}</Text>;
			})}
		</Text>
	);
}

let segmenterModule: any = null;

function requireSegmentize() {
	if (segmenterModule) return segmenterModule;

	try {
		const mod = require("@atcute/bluesky-richtext-segmenter");
		segmenterModule = mod.segmentize;
		return segmenterModule;
	} catch {
		return null;
	}
}
