import { Button, Container, CopyButton, Grid, Group, Stack, Tooltip } from "@mantine/core";
import { LayerImportSection } from "./LayerImportSection";
import { IconCheck, IconPencil, IconReload, IconShare } from "@tabler/icons-react";
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
import { resolvedEventUtils } from "@vantage/core";
import { SmallTitle } from "../../base/SmallTitle";
import { dbShortcuts } from "../../../../db/db-shortcuts";
import { Link } from "@tanstack/react-router";

export interface EventDetailsContentProps {
	loading?: boolean;
	withModalCloseButton?: boolean;
}

export const EventDetailsContent = (props: EventDetailsContentProps) => {
	const { source } = useResolvedEvent();

	return (
		<EventDetailsContext value={props}>
			<Stack gap={0}>
				<EventDetailsBanner />
				<Container w="100%">
					<Stack gap={0}>
						<EnvelopeErrorAlert my="xs" />
						<Group py="xs" gap="xs" justify="end">
							<EventRefetchButton />
							<EventShareButton />
							<EventEditButton />
						</Group>
						<Grid>
							<Grid.Col
								span={{ base: 12, xs: "auto" }}
								order={{ base: 2, xs: 1 }}
							>
								<LayerImportSection />
								<Stack>
									<EventDetailsInstanceList />
									<EventDetailsDescriptionList />
								</Stack>
							</Grid.Col>
							<Grid.Col
								span={{ base: 12, xs: 4 }}
								order={{ base: 1, xs: 2 }}
							>
								<Stack>
									<EventDetailsLinks />
									<EventDetailsSource />
									<EventDetailsAlternatives />
									{/* <EventDetailsRevision /> */}
								</Stack>
							</Grid.Col>
						</Grid>
					</Stack>
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

	return (
		<Tooltip label={"Refetch"} withArrow>
			<AsyncAction action={async () => await dbShortcuts.refetchEvent(id)}>
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

export const EventEditButton = () => {
	const { source, id } = useResolvedEvent();

	if (source.type !== "local" || !id) return null;

	return (
		<Button
			size="compact-sm"
			color="gray"
			leftSection={<IconPencil />}
			renderRoot={(props) => (
				<Link
					to="/edit"
					search={{ id }}
					{...props}
				/>
			)}
		>
			Edit
		</Button>
	);
};
