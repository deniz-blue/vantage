import { EventSourceRegistry } from "../lib/source";

export const resolvedEventUtils = {
	isSourceNetwork(resolved: Vantage.ResolvedEvent): boolean {
		const meta = EventSourceRegistry.get(resolved.source.type);
		return meta?.network ?? false;
	},
	shareLink(resolved: Vantage.ResolvedEvent): string | null {
		const meta = EventSourceRegistry.get(resolved.source.type);
		if (!meta?.shareLink) return null;
		return meta.shareLink(resolved.source as any);
	},
};
