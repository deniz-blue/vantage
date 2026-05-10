import type { schema } from "@vantage/db";
import { ZodError } from "zod";
import type { FailedClientResponse } from "@atcute/client";

export type EventResolveResult = Omit<schema.EventCache, "id" | "updatedAt" | "parsed">;

export interface EventSourceMeta<Type extends keyof Vantage.EventSourceMap> {
	type: Type;
	editable?: boolean;
	network?: boolean;
	resolve?: (source: Vantage.EventSourceMap[Type]) => Promise<EventResolveResult>;
	shareLink?: (source: Vantage.EventSourceMap[Type]) => string | null;
};

export const EventSourceRegistry = new Map<string, EventSourceMeta<any>>();

export const defineEventSource = <Type extends keyof Vantage.EventSourceMap>(o: EventSourceMeta<Type>) => {
	EventSourceRegistry.set(o.type, o);
};

export const fetchEventSource = async (source: Vantage.EventSource): Promise<EventResolveResult> => {
	const meta = EventSourceRegistry.get(source.type);
	if (!meta) throw new Error(`No event source defined for type: ${source.type}`);

	// local data does not have a resolve function
	if (!meta.resolve) return {
		raw: null,
		error: null,
		revision: {},
	};

	try {
		return await meta.resolve(source as any);
	} catch (err) {
		return {
			raw: null,
			error: convertError(err as any),
			revision: {},
		};
	}
};

export const convertError = (err: TypeError | SyntaxError | Response | ZodError | FailedClientResponse): Vantage.Error => {
	const error: Vantage.Error = {
		kind: "unknown",
		message: err instanceof Error ? err.message : "",
		status: (err instanceof Response || ("status" in err)) ? (err.status as number) : undefined,
		issues: ("issues" in err) ? (err.issues as unknown[]) : undefined,
	};

	switch (true) {
		case err instanceof TypeError: error.kind = "fetch"; break;
		case err instanceof SyntaxError: error.kind = "json-parse"; break;
		case err instanceof Response: error.kind = "fetch"; error.status = err.status; break;
		case err instanceof ZodError: error.kind = "validation"; error.issues = err.issues; break;
		case (!(err as FailedClientResponse).ok && !!(err as FailedClientResponse).data): error.kind = "xrpc"; error.message = (err as FailedClientResponse).data.message ?? "Unknown error"; break;
		default: error.kind = "unknown"; break;
	}

	return error;
};
