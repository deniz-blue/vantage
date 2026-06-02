import { sqliteTable, text, integer, primaryKey, blob, customType } from "drizzle-orm/sqlite-core";
import type { EventData } from "@evnt/schema";
import { sql } from "drizzle-orm";
import { jsonb, timestamp, uuid } from "./drizzle-helpers";

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

		interface CredentialTypeMap { }
		type Credential = {
			[Ty in keyof CredentialTypeMap]: CredentialTypeMap[Ty] & { type: Ty };
		}[keyof CredentialTypeMap];
	}
}

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

export const credentials = sqliteTable("credentials", {
	id: uuid("id").primaryKey(),
	data: jsonb("data").$type<Vantage.Credential>().notNull(),
	updatedAt: timestamp("updated_at").notNull(),
});

export type Tag = typeof tags.$inferSelect;
export type EventMeta = typeof eventMeta.$inferSelect;
export type EventCache = typeof eventCache.$inferSelect;
