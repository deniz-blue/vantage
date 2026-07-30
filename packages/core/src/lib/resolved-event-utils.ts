import { EventFormatRegistry, type EventFormat } from "./format";
import { EventSourceRegistry, type EventSourceMeta } from "./source";

export const ResolvedEventUtils = new (class {
	resolveMetadata(resolved: Vantage.ResolvedEvent): {
		source: EventSourceMeta<any>;
		format: EventFormat<any>;
	} {
		const source = EventSourceRegistry.get(resolved.source.type);
		const format = EventFormatRegistry.get(resolved.format.type);

		if (!source) throw new Error(`Unknown event source type: ${resolved.source.type}`);
		if (!format) throw new Error(`Unknown event format type: ${resolved.format.type}`);

		return {
			source,
			format,
		};
	}

	createShareLink(resolved: Vantage.ResolvedEvent): string | null {
		return this.resolveMetadata(resolved).source.shareLink?.(resolved.source as any) ?? null;
	}

	isNetworkSource(resolved: Vantage.ResolvedEvent): boolean {
		return this.resolveMetadata(resolved).source.network ?? false;
	}
})();
