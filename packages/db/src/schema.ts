import { pgTable, timestamp, uuid, jsonb, text, primaryKey } from "drizzle-orm/pg-core";
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
		}
	}
}

export const events = pgTable("events", {
	id: uuid("id").primaryKey().defaultRandom().$type<Vantage.EventId>(),
	updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

export const tags = pgTable("tags", {
	id: uuid("id").primaryKey().defaultRandom(),
	name: text("name").notNull(),
	color: text("color"),
	updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

export const tagHierarchy = pgTable("tag_hierarchy", {
	parentId: uuid("parent_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
	childId: uuid("child_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
}, (table) => ({
	pk: primaryKey({ columns: [table.parentId, table.childId] }),
}));

export const eventTags = pgTable("event_tags", {
	eventId: uuid("event_id").notNull().references(() => events.id, { onDelete: "cascade" }),
	tagId: uuid("tag_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
}, (table) => ({
	pk: primaryKey({ columns: [table.eventId, table.tagId] }),
}));

export const eventMeta = pgTable("event_meta", {
	id: uuid("id").primaryKey().defaultRandom().$type<Vantage.EventId>().references(() => events.id, { onDelete: "cascade" }),
	updatedAt: timestamp("updated_at").defaultNow().notNull(),
	source: jsonb("source").$type<Vantage.EventSource>().notNull(),
	format: jsonb("format").$type<Vantage.EventFormat>().notNull(),
});

export const eventCache = pgTable("event_cache", {
	id: uuid("id").primaryKey().defaultRandom().$type<Vantage.EventId>().references(() => events.id, { onDelete: "cascade" }),
	updatedAt: timestamp("updated_at").defaultNow().notNull(),
	raw: text("raw"),
	parsed: jsonb("parsed").$type<EventData>(),
	revision: jsonb("revision").$type<Vantage.Revision>().notNull(),
	error: jsonb("error").$type<Vantage.Error>(),
});

export type Tag = typeof tags.$inferSelect;
export type EventMeta = typeof eventMeta.$inferSelect;
export type EventCache = typeof eventCache.$inferSelect;
