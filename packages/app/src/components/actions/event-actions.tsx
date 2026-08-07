import { FOLIO_BASE_URL, ResolvedEventUtils } from "@vantage/core";
import { Action } from "./action";
import { renderMarkdown } from "@evnt/pretty";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { IconSize } from "../../theme/sizing";
import { IconUserEdit } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";

export const createActionsForEvent = (resolved: Vantage.ResolvedEvent) => {
	const actions: Action[] = [];

	if (resolved.error && resolved.data)
		actions.push({
			label: "View: Resolved Data",
			type: "raw",
			value: JSON.stringify(resolved.data),
		});

	if (resolved.error && resolved.raw)
		actions.push({
			label: "View: Raw Data",
			type: "raw",
			value: resolved.raw,
		});

	if (resolved.error && resolved.format.type !== "directory.evnt.event" && resolved.raw)
		actions.push({
			label: "Copy Raw Data",
			type: "copy",
			value: resolved.raw ?? "",
		});

	if (resolved.error && resolved.source.type === "http")
		actions.push({
			label: "Copy Data URL",
			type: "copy",
			value: resolved.source.url,
		});

	if (resolved.error && resolved.source.type === "at")
		actions.push({
			label: "Copy at-uri",
			type: "copy",
			value: resolved.source.uri,
		});

	const shareLink = ResolvedEventUtils.createShareLink(resolved);
	if (shareLink)
		actions.push({
			label: "Share",
			type: "share",
			value: shareLink,
		});

	if (resolved.data)
		actions.push({
			label: "Copy Summary",
			type: "copy",
			value: () =>
				renderMarkdown(resolved.data!, {
					timezone: useLocaleStore.getState().timezone,
					language: useLocaleStore.getState().language,
				}),
		});

	if (resolved.source.type === "folio" && resolved.source.editToken) {
		const editUrl = new URL(
			`/events/${resolved.source.id}`,
			resolved.source.baseUrl ?? FOLIO_BASE_URL,
		);
		editUrl.searchParams.set("token", resolved.source.editToken);
		actions.push({
			label: "Share Editing Link",
			type: "share",
			value: editUrl.toString(),
			icon: <IconUserEdit size={IconSize.xs} color={Colors.Text} />,
		});
	}

	return actions;
};
