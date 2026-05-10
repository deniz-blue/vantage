import { EventSourceRegistry } from "@vantage/core";
import { ResolvedEvent } from "../db/resolved-event";

export const resolvedEventUtils = {
	shareLink(resolved: ResolvedEvent): string | null {
		const meta = EventSourceRegistry.get(resolved.source.type);
		if (!meta?.shareLink) return null;
		return meta.shareLink(resolved.source as any);
	},
};
