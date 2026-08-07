import { ZodError } from "zod";
import { EventFormatRegistry, inferFormatFromRaw } from "./format";
import { EventSourceRegistry } from "./source";
import { ClientResponseError } from "@atcute/client";
import { db, schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { createComputedData } from "../database/computed";

export const asyncPipe = <T>(...fns: ((arg: T) => Promise<T>)[]) =>
	fns.reduce((first, second) => (input) => first(input).then((x) => second(x)));

export const EventResolver = new (class {
	// Factory

	new(resolved?: Partial<Vantage.ResolvedEvent>): Vantage.ResolvedEvent {
		return {
			id: null,
			error: null,
			raw: null,
			data: null,
			format: { type: "unknown" },
			source: { type: "unknown" },
			revision: {},
			...resolved,
		};
	}

	// Helpers

	withError(resolved: Vantage.ResolvedEvent, error: Vantage.Error): Vantage.ResolvedEvent {
		return {
			...resolved,
			error,
		};
	}

	fromDatabase(
		resolved: Vantage.ResolvedEvent,
		result: { event_meta: schema.EventMeta; event_cache: schema.EventCache | null },
	): Vantage.ResolvedEvent {
		const {
			event_meta: { source, format },
			event_cache: cached,
		} = result;

		return {
			...resolved,
			id: result.event_meta.id,
			source,
			format,
			data: cached?.parsed || resolved.data || null,
			raw: cached?.raw || resolved.raw || null,
			error: cached?.error || resolved.error || null,
			revision: cached?.revision || resolved.revision || {},
			updatedAt: cached?.updatedAt || resolved.updatedAt,
		};
	}

	convertError(
		err: TypeError | SyntaxError | Response | ZodError | ClientResponseError,
	): Vantage.Error {
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
			default:
				error.kind = "unknown";
				break;
		}

		return error;
	}

	mustFetch(resolved: Vantage.ResolvedEvent): boolean {
		// if source is unknown, we have no way to fetch, so skip
		if (resolved.source.type === "unknown") return false;

		// if we have raw data, we can skip fetching and go straight to parsing
		if (resolved.raw) return false;

		return true;
	}

	mustParse(resolved: Vantage.ResolvedEvent): boolean {
		// if format is unknown, we have no way to parse, so skip
		if (resolved.format.type === "unknown") return false;

		// if we have data, we can skip parsing
		if (resolved.data) return false;

		return true;
	}

	// Pipeline

	async selectFromDatabase(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!resolved.id) return resolved;

		const result = await db
			.select()
			.from(schema.eventMeta)
			.leftJoin(schema.eventCache, eq(schema.eventMeta.id, schema.eventCache.id))
			.where(eq(schema.eventMeta.id, resolved.id))
			.then((rows) => rows[0]);

		if (!result) return resolved;

		return EventResolver.fromDatabase(resolved, result);
	}

	async upsertToDatabase(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!resolved.id || resolved.source.type === "local" || !resolved.data) return resolved;

		const values: schema.EventCache = {
			id: resolved.id,
			error: resolved.error,
			parsed: resolved.data,
			raw: resolved.raw,
			revision: resolved.revision,
			updatedAt: resolved.updatedAt || Temporal.Now.instant(),
			computed: createComputedData(resolved.data),
		};

		// can continue in the background maybe
		await db.insert(schema.eventCache).values(values).onConflictDoUpdate({
			target: schema.eventCache.id,
			set: values,
		});

		return resolved;
	}

	async fetch(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		const source = EventSourceRegistry.get(resolved.source.type);
		if (!source)
			return EventResolver.withError(resolved, {
				kind: "unknown-source",
				message: `No event source defined for type: ${resolved.source.type}`,
			});

		// local data does not have a resolve function
		if (!source.resolve) return resolved;

		try {
			const result = await source.resolve(resolved.source);
			
			// sniff format from raw data if still unknown
			let format = resolved.format;
			if (format.type === "unknown" && result.raw) {
				format = inferFormatFromRaw(result.raw, resolved.source) ?? format;
			}

			return {
				...resolved,
				raw: result.raw,
				error: result.error,
				revision: result.revision,
				format,
				updatedAt: Temporal.Now.instant(),
			};
		} catch (err) {
			return EventResolver.withError(resolved, EventResolver.convertError(err as any));
		}
	}

	async fetchIfNeeded(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!EventResolver.mustFetch(resolved)) return resolved;
		return await EventResolver.fetch(resolved);
	}

	async parse(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!resolved.raw || resolved.format.type === "unknown") return resolved;

		const format = EventFormatRegistry.get(resolved.format.type);
		if (!format) return resolved;

		try {
			const result = format.parse({
				format: resolved.format,
				raw: resolved.raw,
				source: resolved.source,
			});
			return {
				...resolved,
				data: result.parsed,
				error: resolved.error || result.error,
			};
		} catch (e) {
			return EventResolver.withError(resolved, EventResolver.convertError(e as any));
		}
	}

	async parseIfNeeded(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!EventResolver.mustParse(resolved)) return resolved;
		return await EventResolver.parse(resolved);
	}
})();
