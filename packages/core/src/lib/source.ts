import type { schema } from "@vantage/db";
import { ZodError } from "zod";
import { ClientResponseError, type FailedClientResponse } from "@atcute/client";

export type EventResolveResult = Omit<
	schema.EventCache,
	"id" | "updatedAt" | "parsed" | "computed"
>;

export interface EventSourceMeta<Type extends keyof Vantage.EventSourceMap> {
	type: Type;
	editable?: boolean;
	network?: boolean;
	resolve?: (source: Vantage.EventSourceMap[Type]) => Promise<EventResolveResult>;
	shareLink?: (source: Vantage.EventSourceMap[Type]) => string | null;
}

export const EventSourceRegistry = new Map<string, EventSourceMeta<any>>();

export const defineEventSource = <Type extends keyof Vantage.EventSourceMap>(
	o: EventSourceMeta<Type>,
) => {
	EventSourceRegistry.set(o.type, o);
};

export const convertError = (
	err: TypeError | SyntaxError | Response | ZodError | FailedClientResponse,
): Vantage.Error => {
	const error: Vantage.Error = {
		kind: "unknown",
		message: err instanceof Error ? err.message : "",
		status: err instanceof Response || "status" in err ? (err.status as number) : undefined,
		issues: "issues" in err ? (err.issues as unknown[]) : undefined,
	};

	switch (true) {
		case err instanceof TypeError:
			error.kind = "fetch";
			break;
		case err instanceof SyntaxError:
			error.kind = "json-parse";
			break;
		case err instanceof Response:
			error.kind = "fetch";
			error.status = err.status;
			break;
		case err instanceof ZodError:
			error.kind = "validation";
			error.issues = err.issues;
			break;
		case err instanceof ClientResponseError:
			error.kind = "xrpc";
			error.message = err.message;
			error.status = err.status;
			break;
		case !(err as FailedClientResponse).ok && !!(err as FailedClientResponse).data:
			error.kind = "xrpc";
			error.message = (err as FailedClientResponse).data.message ?? "Unknown error";
			break;
		default:
			error.kind = "unknown";
			break;
	}

	return error;
};
