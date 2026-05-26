import { ZodError } from "zod";
import { EventFormatRegistry } from "./format";
import { EventSourceRegistry } from "./source";
import { ClientResponseError } from "@atcute/client";
import { db, schema } from "@vantage/db";
import { eq } from "drizzle-orm";
import { createComputedData } from "../database/computed";

export const asyncPipe = <T>(...fns: ((arg: T) => Promise<T>)[]) =>
	fns.reduce((first, second) => (input) => first(input).then((x) => second(x)));

export const EventResolver = new class {
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

	convertError(err: TypeError | SyntaxError | Response | ZodError | ClientResponseError): Vantage.Error {
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
			case err instanceof ClientResponseError: error.kind = "xrpc"; error.message = err.message; error.status = err.status; break;
			default: error.kind = "unknown"; break;
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
			.then(rows => rows[0]);

		if (!result) return resolved;

		const {
			event_meta: { source, format },
			event_cache: cached,
		} = result;

		return {
			...resolved,
			source,
			format,
			data: cached?.parsed || null,
			raw: cached?.raw || null,
			error: cached?.error || null,
			revision: cached?.revision || {},
		};
	}

	async upsertToDatabase(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!resolved.id || resolved.source.type === "local" || !resolved.data) return resolved;

		const values: schema.EventCache = {
			id: resolved.id,
			error: resolved.error,
			parsed: resolved.data,
			raw: resolved.raw,
			revision: resolved.revision,
			updatedAt: Temporal.Now.instant(),
			computed: createComputedData(resolved.data),
		};

		// This can continue in the background maybe
		await db
			.insert(schema.eventCache)
			.values(values)
			.onConflictDoUpdate({
				target: schema.eventCache.id,
				set: values,
			});

		return resolved;
	}

	async fetch(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		const source = EventSourceRegistry.get(resolved.source.type);
		if (!source) return this.withError(resolved, {
			kind: "unknown-source",
			message: `No event source defined for type: ${resolved.source.type}`,
		});

		// local data does not have a resolve function
		if (!source.resolve) return resolved;

		try {
			const result = await source.resolve(resolved.source);
			return {
				...resolved,
				raw: result.raw,
				error: result.error,
				revision: result.revision,
			};
		} catch (err) {
			return this.withError(resolved, this.convertError(err as any));
		}
	}

	async fetchIfNeeded(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!this.mustFetch(resolved)) return resolved;
		return await this.fetch(resolved);
	}

	async parse(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!resolved.raw || resolved.format.type === "unknown") return resolved;

		const format = EventFormatRegistry.get(resolved.format.type);
		if (!format) return resolved;

		try {
			const result = format.parse(resolved.raw, format, { source: resolved.source });
			return {
				...resolved,
				data: result.parsed,
				error: resolved.error || result.error,
			};
		} catch (e) {
			return this.withError(resolved, this.convertError(e as any));
		}
	}

	async parseIfNeeded(resolved: Vantage.ResolvedEvent): Promise<Vantage.ResolvedEvent> {
		if (!this.mustParse(resolved)) return resolved;
		return await this.parse(resolved);
	}
};
