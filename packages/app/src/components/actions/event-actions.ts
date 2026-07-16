import { ResolvedEventUtils } from "@vantage/core";
import { Action } from "./action";
import { renderMarkdown } from "@evnt/pretty";
import { useLocaleStore } from "../../stores/useLocaleStore";

export const createActionsForEvent = (resolved: Vantage.ResolvedEvent) => {
	const actions: Action[] = [];

	if (resolved.data)
		actions.push({
			label: "View: Resolved Data",
			type: "raw",
			value: JSON.stringify(resolved.data),
		});

	if (resolved.raw)
		actions.push({
			label: "View: Raw Data",
			type: "raw",
			value: resolved.raw,
		});

	if (resolved.format.type !== "directory.evnt.event" && resolved.raw)
		actions.push({
			label: "Copy Raw Data",
			type: "copy",
			value: resolved.raw ?? "",
		});

	if (resolved.source.type === "http")
		actions.push({
			label: "Copy Data URL",
			type: "copy",
			value: resolved.source.url,
		});

	if (resolved.source.type === "at")
		actions.push({
			label: "Copy at-uri",
			type: "copy",
			value: resolved.source.uri,
		});

	const shareLink = ResolvedEventUtils.createShareLink(resolved);
	if (shareLink)
		actions.push({
			label: "Copy Share Link",
			type: "copy",
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

	return actions;
};
