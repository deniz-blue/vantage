import { sqliteTable, text, integer, primaryKey, blob, customType } from "drizzle-orm/sqlite-core";
import type { EventData } from "@evnt/schema";
import { sql } from "drizzle-orm";

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

		interface ComputedData {
			timeRanges?: {
				low: number;
				high: number;
			}[];
		}
	}
}

const uuid = (name: string) => text(name, { length: 36 });

const timestamp = customType<{
	data: Temporal.Instant;
	driverData: number;
}>({
	dataType: () => "integer",
	fromDriver: (value) => Temporal.Instant.fromEpochMilliseconds(value),
	toDriver: (value) => value.epochMilliseconds,
});

// 2.45+ supports JSON mode for blob
const jsonb = customType<{
	data: any;
	driverData: string;
}>({
	dataType: () => "blob",
	fromDriver: (value) => JSON.parse(value),
	toDriver: (value) => JSON.stringify(value),
});

export const events = sqliteTable("events", {
	id: uuid("id").primaryKey().$type<Vantage.EventId>(),
	updatedAt: timestamp("updated_at").notNull(),
});

export const tags = sqliteTable("tags", {
	id: uuid("id").primaryKey().$type<Vantage.EventId>(),
	name: text("name").notNull(),
	color: text("color"),
	updatedAt: timestamp("updated_at").notNull(),
});

export const tagHierarchy = sqliteTable("tag_hierarchy", {
	parentId: uuid("parent_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
	childId: uuid("child_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
}, (table) => ({
	pk: primaryKey({ columns: [table.parentId, table.childId] }),
}));

export const eventTags = sqliteTable("event_tags", {
	eventId: uuid("event_id").notNull().references(() => events.id, { onDelete: "cascade" }),
	tagId: uuid("tag_id").notNull().references(() => tags.id, { onDelete: "cascade" }),
}, (table) => ({
	pk: primaryKey({ columns: [table.eventId, table.tagId] }),
}));

export const eventMeta = sqliteTable("event_meta", {
	id: uuid("id").primaryKey().$type<Vantage.EventId>().references(() => events.id, { onDelete: "cascade" }),
	updatedAt: timestamp("updated_at").notNull(),
	source: jsonb("source").$type<Vantage.EventSource>().notNull(),
	format: jsonb("format").$type<Vantage.EventFormat>().notNull(),
});

export const eventCache = sqliteTable("event_cache", {
	id: uuid("id").primaryKey().$type<Vantage.EventId>().references(() => events.id, { onDelete: "cascade" }),
	updatedAt: timestamp("updated_at").notNull(),
	raw: text("raw"),
	parsed: jsonb("parsed").$type<EventData>(),
	revision: jsonb("revision").$type<Vantage.Revision>().notNull().default(sql`'{}'`),
	error: jsonb("error").$type<Vantage.Error>(),
	computed: jsonb("computed").$type<Vantage.ComputedData>().notNull().default(sql`'{}'`),
});

export type Tag = typeof tags.$inferSelect;
export type EventMeta = typeof eventMeta.$inferSelect;
export type EventCache = typeof eventCache.$inferSelect;
