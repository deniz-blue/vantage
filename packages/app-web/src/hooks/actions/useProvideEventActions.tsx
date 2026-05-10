import { useProvideActionList } from "../../components/app/overlay/spotlight/useAction";
import { handleAsyncCopy, handleCopy } from "../../lib/util/copy";
import { IconBraces, IconClipboard, IconCode, IconEdit, IconJson, IconMarkdown, IconQrcode, IconReload, IconShare, IconTrash } from "@tabler/icons-react";
import { useNavigate } from "@tanstack/react-router";
import { Code } from "@mantine/core";
import { QRCode } from "../../lib/util/qrcode";
import { modals } from "@mantine/modals";
import { AsyncLoader } from "../../components/data/AsyncLoader";
import type { ResolvedEvent } from "../../db/resolved-event";
import { EventSourceRegistry } from "@vantage/core";
import { resolvedEventUtils } from "../../lib/resolved-utils";

export const EventActionFactory = {
	All: ({
		resolved,
		navigate,
	}: {
		resolved: ResolvedEvent;
		navigate: ReturnType<typeof useNavigate>;
	}) => [
			EventActionFactory.Edit(navigate, resolved),
			EventActionFactory.CopyShareLink(resolved),
			EventActionFactory.CopyMarkdownLink(resolved),
			EventActionFactory.ShareLinkQRCode(resolved),
			EventActionFactory.CopySourceHttp(resolved),
			EventActionFactory.CopyAtUri(resolved),
			EventActionFactory.CopyJSON(resolved),
			EventActionFactory.ViewRawJSON(resolved),
			EventActionFactory.ViewResolvedJSON(resolved),
			// EventActionFactory.RefetchData(resolved),
			// EventActionFactory.CopyEmbedLink(resolved),
			// EventActionFactory.ViewOnPDS(resolved),
			// EventActionFactory.Delete(resolved),
		],

	Edit: (navigate: ReturnType<typeof useNavigate>, resolved: ResolvedEvent) => ({
		label: "Edit",
		icon: <IconEdit />,
		disabled: !EventSourceRegistry.get(resolved.source.type)?.editable,
		category: "Event",
		execute: () => navigate({
			to: "/edit",
			search: {
				id: resolved.id,
			},
		}),
	}),
	CopyShareLink: (resolved: ResolvedEvent) => ({
		label: "Copy Share Link",
		icon: <IconShare />,
		disabled: !resolvedEventUtils.shareLink(resolved),
		category: "Event",
		execute: handleCopy(
			resolvedEventUtils.shareLink(resolved) ?? "",
			"Event share link copied to clipboard",
		),
		id: "copy-event-share-link",
	}),
	CopyMarkdownLink: (resolved: ResolvedEvent) => ({
		label: "Copy Markdown Link",
		icon: <IconMarkdown />,
		disabled: !resolvedEventUtils.shareLink(resolved),
		category: "Event",
		execute: handleAsyncCopy(
			async () => {
				const data = resolved.data;
				const name = data?.name["en"] ?? data?.name[Object.keys(data.name)[0]!] ?? "Event";
				return `[${name}](${resolvedEventUtils.shareLink(resolved)!})`;
			},
			"Event share link copied to clipboard",
		),
		id: "copy-event-markdown-link",
		deps: [resolved.id],
	}),
	ShareLinkQRCode: (resolved: ResolvedEvent) => ({
		label: "Share (QR)",
		icon: <IconQrcode />,
		disabled: !resolvedEventUtils.shareLink(resolved),
		category: "Event",
		execute: () => modals.open({
			size: "md",
			children: (
				<QRCode value={resolvedEventUtils.shareLink(resolved)!} />
			),
		}),
		id: "share-link-qrcode",
		deps: [resolved.id],
	}),
	CopySourceHttp: (resolved: ResolvedEvent) => ({
		label: "Copy Source",
		icon: <IconClipboard />,
		disabled: resolved.source.type !== "http",
		category: "Event",
		execute: handleCopy(
			(resolved.source.type === "http" ? resolved.source.url : "") ?? "",
			"Event source copied to clipboard",
		),
		id: "copy-event-source",
		deps: [resolved.id],
	}),
	CopyAtUri: (resolved: ResolvedEvent) => ({
		label: "Copy AT URI",
		icon: <IconClipboard />,
		disabled: resolved.source.type !== "at",
		category: "Event",
		execute: handleCopy(
			(resolved.source.type === "at" ? resolved.source.uri : "") ?? "",
			"Event AT URI copied to clipboard",
		),
		id: "copy-event-at-uri",
		deps: [resolved.id],
	}),
	CopyJSON: (resolved: ResolvedEvent) => ({
		label: "Copy Parsed JSON",
		category: "Event",
		icon: <IconBraces />,
		execute: handleAsyncCopy(
			async (): Promise<string> => JSON.stringify(resolved.data, null, 2) ?? "",
			"Event JSON copied to clipboard",
		),
		id: "copy-event-json",
		deps: [resolved.id],
	}),
	ViewRawJSON: (resolved: ResolvedEvent) => ({
		label: "View JSON (Original)",
		category: "Event",
		icon: <IconJson />,
		execute: () => modals.open({
			size: "xl",
			title: "JSON Data",
			children: (
				<Code block>
					{resolved.raw ?? "No data"}
				</Code>
			),
		}),
		id: "view-event-raw-json",
		deps: [resolved.id],
	}),
	ViewResolvedJSON: (resolved: ResolvedEvent) => ({
		label: "View JSON (Resolved)",
		category: "Event",
		icon: <IconJson />,
		execute: () => modals.open({
			size: "xl",
			title: "JSON Data",
			children: (
				<AsyncLoader fetcher={async () => resolved.data}>
					{(o) => (
						<Code block>
							{JSON.stringify(o, null, 2) ?? "No data"}
						</Code>
					)}
				</AsyncLoader>
			),
		}),
		id: "view-event-resolved-json",
		deps: [resolved.id],
	}),
	// TODO
	// RefetchData: (resolved: ResolvedEvent) => ({
	// 	label: "Refetch",
	// 	category: "Event",
	// 	icon: <IconReload />,
	// 	disabled: !UtilEventSource.isFromNetwork(resolved.source),
	// 	execute: () => EventResolver.update(resolved.source),
	// 	id: "refetch-event",
	// 	deps: [resolved.id],
	// }),
	// CopyEmbedLink: (resolved: ResolvedEvent) => ({
	// 	label: "Copy Embed Link",
	// 	category: "Event",
	// 	icon: <IconCode />,
	// 	disabled: !UtilEventSource.isFromNetwork(resolved.source),
	// 	execute: handleCopy(
	// 		EventActions.getEmbedLink(resolved.source) ?? "",
	// 		"Event embed link copied to clipboard",
	// 	),
	// 	id: "copy-event-embed-link",
	// 	deps: [resolved.id],
	// }),
	// ViewOnPDS: (resolved: ResolvedEvent) => ({
	// 	label: "View on pds.ls",
	// 	category: "Event",
	// 	icon: <PDSlsIcon />,
	// 	disabled: UtilEventSource.getType(resolved.source) !== "at",
	// 	execute: () => window.open(`https://pds.ls/${resolved.source}`, "_blank"),
	// 	special: {
	// 		href: `https://pds.ls/${resolved.source}`,
	// 	},
	// }),
	// Delete: (resolved: ResolvedEvent) => ({
	// 	label: UtilEventSource.isLocal(resolved.source) ? "Delete" : "Remove",
	// 	category: "Event",
	// 	icon: <IconTrash />,
	// 	disabled: UtilEventSource.isAt(resolved.source),
	// 	execute: () => openConfirmModal(
	// 		UtilEventSource.isLocal(resolved.source) ? "Are you sure you want to delete this event?" : "Are you sure you want to stop following this event?",
	// 		() => EventActions.deleteEvent(resolved.source),
	// 	),
	// 	id: "delete-event",
	// 	special: {
	// 		color: "red",
	// 	},
	// 	deps: [resolved.id],
	// }),
};

export const useProvideEventActions = (resolved?: ResolvedEvent) => {
	const navigate = useNavigate();
	useProvideActionList(resolved ? EventActionFactory.All({
		resolved,
		navigate,
	}) : []);
};
