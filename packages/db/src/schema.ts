import { pgTable, timestamp, uuid, jsonb, text } from "drizzle-orm/pg-core";
import type { EventData } from "@evnt/schema";

declare global {
	namespace Vantage {
		type EventId = `${string}-${string}-${string}-${string}-${string}`;
		
		interface EventSourceMap { }
		type EventSource = {
			[Ty in keyof EventSourceMap]: EventSourceMap[Ty] & { type: Ty };
		}[keyof EventSourceMap];

		interface EventFormatMap { }
		type EventFormat = {
			[Ty in keyof EventFormatMap]: EventFormatMap[Ty] & { type: Ty };
		}[keyof EventFormatMap];

		interface Revision { }
		interface Error {
			kind: string;
			message: string;
			status?: number;
			issues?: any[];
			error?: string;
			dataType?: string;
		}
	}
}

export const eventMeta = pgTable("event_meta", {
	id: uuid("id").primaryKey().defaultRandom().$type<Vantage.EventId>(),
	updatedAt: timestamp("updated_at").defaultNow().notNull(),
	source: jsonb("source").$type<Vantage.EventSource>().notNull(),
	format: jsonb("format").$type<Vantage.EventFormat>().notNull(),
});

export const eventCache = pgTable("event_cache", {
	id: uuid("id").primaryKey().defaultRandom().$type<Vantage.EventId>().references(() => eventMeta.id),
	updatedAt: timestamp("updated_at").defaultNow().notNull(),
	raw: text("raw"),
	parsed: jsonb("parsed").$type<EventData>(),
	revision: jsonb("revision").$type<Vantage.Revision>().notNull(),
	error: jsonb("error").$type<Vantage.Error>(),
});

export type EventMeta = typeof eventMeta.$inferSelect;
export type EventCache = typeof eventCache.$inferSelect;
