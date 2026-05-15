import { ActionIcon, Box, Button, Code, Container, CopyButton, Grid, Group, Stack, Text, Tooltip } from "@mantine/core";
import { LayerImportSection } from "./LayerImportSection";
import { IconCheck, IconReload, IconShare } from "@tabler/icons-react";
import { AsyncAction } from "../../../data/AsyncAction";
import { EventDetailsContext } from "./event-details-context";
import { EventDetailsBanner } from "./EventDetailsBanner";
import { EventDetailsInstanceList } from "./EventDetailsInstanceList";
import { EventDetailsLinks } from "./EventDetailsLinks";
import { EnvelopeErrorAlert } from "../envelope/EnvelopeErrorAlert";
import { EventDetailsSource } from "./EventDetailsSource";
import { EventDetailsAlternatives } from "./EventDetailsAlternatives";
import { RichTextRenderer } from "./RichTextRenderer";
import type { Facet } from "@atcute/bluesky-richtext-segmenter";
import { useResolvedEvent } from "@vantage/core";
import { EventSourceRegistry } from "@vantage/core";
import { resolvedEventUtils } from "@vantage/core";
import { SmallTitle } from "../../base/SmallTitle";
import { EventDetailsRevision } from "./EventDetailsRevision";

export interface EventDetailsContentProps {
	loading?: boolean;
	withModalCloseButton?: boolean;
}

export const EventDetailsContent = (props: EventDetailsContentProps) => {
	const { source } = useResolvedEvent();

	const sourceMeta = EventSourceRegistry.get(source.type);

	return (
		<EventDetailsContext value={props}>
			<Stack gap="xs">
				<EventDetailsBanner />
				<Container w="100%">
					<Stack>
						<EnvelopeErrorAlert my="xs" />
					</Stack>

					<Grid>
						<Grid.Col
							span={{ base: 12, md: "auto" }}
							order={{ base: 1, md: 2 }}
						>
							<Stack>
								<Group gap="xs" justify="end">
									<EventRefetchButton />
									<EventShareButton />
								</Group>
								<LayerImportSection />
								<EventDetailsInstanceList />
								<EventDetailsDescriptionList />
							</Stack>
						</Grid.Col>
						<Grid.Col
							span={{ base: 12, md: 4 }}
							order={{ base: 2, md: 1 }}
						>
							<Stack>
								<EventDetailsLinks />
								<EventDetailsSource />
								<EventDetailsAlternatives />
								{/* <EventDetailsRevision /> */}
							</Stack>
						</Grid.Col>
					</Grid>
				</Container>
			</Stack>
		</EventDetailsContext>
	);
};

export const EventDetailsDescriptionList = () => {
	const { data } = useResolvedEvent();

	const bskyRichTextComp = data?.components?.find((x): x is { $type: "app.bsky.richtext"; text: string; facets?: Facet[] } =>
		x.$type === "app.bsky.richtext" && typeof (x as { text?: unknown }).text === "string");

	if (!bskyRichTextComp) return null;

	return (
		<Stack gap="0" component="section">
			<SmallTitle>
				description
			</SmallTitle>

			<RichTextRenderer content={bskyRichTextComp.text} facets={bskyRichTextComp.facets ?? []} />
		</Stack>
	)
};

export const EventRefetchButton = () => {
	const { id } = useResolvedEvent();

	if (!id) return null;

	// TODO
	return (
		<Tooltip label={"Refetch"} withArrow>
			<AsyncAction action={async () => { }}>
				{({ loading, onClick }) => (
					<Button
						size="compact-sm"
						color="gray"
						onClick={onClick}
						leftSection={<IconReload />}
						loading={loading}
					>
						Refetch
					</Button>
				)}
			</AsyncAction>
		</Tooltip>
	);
};

export const EventShareButton = () => {
	const resolved = useResolvedEvent();

	const shareLink = resolvedEventUtils.shareLink(resolved);

	if (!shareLink) return null;

	return (
		<CopyButton value={shareLink}>
			{({ copied, copy }) => (
				<Button
					size="compact-sm"
					color={copied ? "green" : "gray"}
					onClick={copy}
					leftSection={copied ? <IconCheck /> : <IconShare />}
				>
					{copied ? "Copied!" : "Share"}
				</Button>
			)}
		</CopyButton>
	);
};
