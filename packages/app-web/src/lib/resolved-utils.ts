import { EventSourceRegistry } from "@vantage/core";
import { ResolvedEvent } from "../db/resolved-event";

export const resolvedEventUtils = {
	isSourceNetwork(resolved: ResolvedEvent): boolean {
		const meta = EventSourceRegistry.get(resolved.source.type);
		return meta?.network ?? false;
	},
	shareLink(resolved: ResolvedEvent): string | null {
		const meta = EventSourceRegistry.get(resolved.source.type);
		if (!meta?.shareLink) return null;
		return meta.shareLink(resolved.source as any);
	},
};
