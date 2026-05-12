import { useProvideActionList } from "../../components/app/overlay/spotlight/useAction";
import { handleAsyncCopy, handleCopy } from "../../lib/util/copy";
import { IconBug, IconClipboard, IconCode, IconEdit, IconJson, IconMarkdown, IconQrcode, IconShare, IconTrash } from "@tabler/icons-react";
import { useNavigate } from "@tanstack/react-router";
import { QRCode } from "../../lib/util/qrcode";
import { modals } from "@mantine/modals";
import { EventSourceRegistry } from "@vantage/core";
import { resolvedEventUtils } from "@vantage/core";
import { notifications } from "@mantine/notifications";
import { openConfirmModal } from "../../lib/util/confirm";
import { dbShortcuts } from "../../db/db-shortcuts";
import { PDSlsIcon } from "../../lib/resources/PDSlsIcon";

export const EventActionFactory = {
	All: ({
		resolved,
		navigate,
	}: {
		resolved: Vantage.ResolvedEvent;
		navigate: ReturnType<typeof useNavigate>;
	}) => [
			EventActionFactory.Edit(navigate, resolved),
			EventActionFactory.CopyShareLink(resolved),
			EventActionFactory.CopyMarkdownLink(resolved),
			EventActionFactory.ShareLinkQRCode(resolved),
			EventActionFactory.CopySourceHttp(resolved),
			EventActionFactory.CopyAtUri(resolved),
			EventActionFactory.ViewRaw(resolved),
			EventActionFactory.ViewParsed(resolved),
			EventActionFactory.ViewDebug(resolved),
			// EventActionFactory.RefetchData(resolved),
			// EventActionFactory.CopyEmbedLink(resolved),
			EventActionFactory.ViewOnPDS(resolved),
			EventActionFactory.Delete(navigate, resolved),
		],

	Edit: (navigate: ReturnType<typeof useNavigate>, resolved: Vantage.ResolvedEvent) => ({
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
	CopyShareLink: (resolved: Vantage.ResolvedEvent) => ({
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
	CopyMarkdownLink: (resolved: Vantage.ResolvedEvent) => ({
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
	ShareLinkQRCode: (resolved: Vantage.ResolvedEvent) => ({
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
	CopySourceHttp: (resolved: Vantage.ResolvedEvent) => ({
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
	CopyAtUri: (resolved: Vantage.ResolvedEvent) => ({
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
	ViewRaw: (resolved: Vantage.ResolvedEvent) => ({
		label: "View Original",
		category: "Event",
		icon: <IconJson />,
		execute: () => modals.openContextModal({
			modal: "CodeBlockModal",
			centered: true,
			withCloseButton: false,
			size: "xl",
			innerProps: {
				raw: resolved.raw ?? "",
			},
		}),
		id: "view-event-raw",
		deps: [resolved.id],
	}),
	ViewParsed: (resolved: Vantage.ResolvedEvent) => ({
		label: "View OpenEvnt JSON",
		category: "Event",
		icon: <IconJson />,
		execute: () => modals.openContextModal({
			modal: "CodeBlockModal",
			centered: true,
			withCloseButton: false,
			size: "xl",
			innerProps: {
				raw: JSON.stringify(resolved.data) ?? "",
			},
		}),
		id: "view-event-parsed",
		deps: [resolved.id],
	}),
	ViewDebug: (resolved: Vantage.ResolvedEvent) => ({
		label: "Debug",
		category: "Event",
		icon: <IconBug />,
		execute: () => modals.openContextModal({
			modal: "CodeBlockModal",
			centered: true,
			withCloseButton: false,
			size: "xl",
			innerProps: {
				raw: JSON.stringify(resolved) ?? "",
			},
		}),
		id: "view-event-debug",
		deps: [resolved.id],
	}),
	// RefetchData: (resolved: ResolvedEvent) => ({
	// 	label: "Refetch",
	// 	category: "Event",
	// 	icon: <IconReload />,
	// 	disabled: !UtilEventSource.isFromNetwork(resolved.source),
	// 	execute: () => EventResolver.update(resolved.source),
	// 	id: "refetch-event",
	// 	deps: [resolved.id],
	// }),
	// TODO
	// CopyEmbedLink: (resolved: ResolvedEvent) => ({
	// 	label: "Copy Embed Link",
	// 	category: "Event",
	// 	icon: <IconCode />,
	// 	disabled: !resolvedEventUtils.isSourceNetwork(resolved),
	// 	execute: handleCopy(
	// 		EventActions.getEmbedLink(resolved.source) ?? "",
	// 		"Event embed link copied to clipboard",
	// 	),
	// 	id: "copy-event-embed-link",
	// 	deps: [resolved.id],
	// }),
	ViewOnPDS: (resolved: Vantage.ResolvedEvent) => ({
		label: "View on pds.ls",
		category: "Event",
		icon: <PDSlsIcon />,
		disabled: resolved.source.type !== "at",
		execute: () => window.open(`https://pds.ls/${(resolved.source as any).uri}`, "_blank"),
		special: {
			href: `https://pds.ls/${(resolved.source as any).uri}`,
		},
	}),
	Delete: (navigate: ReturnType<typeof useNavigate>, resolved: Vantage.ResolvedEvent) => ({
		label: resolvedEventUtils.isSourceNetwork(resolved) ? "Stop Following" : "Delete",
		category: "Event",
		icon: <IconTrash />,
		disabled: !resolved.id,
		execute: () => openConfirmModal(
			resolvedEventUtils.isSourceNetwork(resolved) ? "Are you sure you want to stop following this event?" : "Are you sure you want to delete this event? This action cannot be undone.",
			async () => {
				if (!resolved.id) return;
				const id = `delete::${resolved.id}`;
				notifications.show({
					id,
					message: "Deleting event...",
					loading: true,
				});
				await dbShortcuts.deleteEventMeta(resolved.id);
				notifications.update({
					id,
					message: "Event deleted",
					loading: false,
					color: "red",
				});
			},
		),
		id: "delete-event",
		special: {
			color: "red",
		},
		deps: [resolved.id],
	}),
};

export const useProvideEventActions = (resolved?: Vantage.ResolvedEvent) => {
	const navigate = useNavigate();
	useProvideActionList(resolved ? EventActionFactory.All({
		resolved,
		navigate,
	}) : []);
};
