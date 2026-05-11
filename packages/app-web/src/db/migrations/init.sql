CREATE TABLE IF NOT EXISTS "events" (
	"id" text PRIMARY KEY NOT NULL,
	"updated_at" integer DEFAULT (strftime('%s', 'now') * 1000) NOT NULL
);
CREATE TABLE IF NOT EXISTS "tags" (
	"id" text PRIMARY KEY NOT NULL,
	"name" text NOT NULL,
	"color" text,
	"updated_at" integer DEFAULT (strftime('%s', 'now') * 1000) NOT NULL
);
CREATE TABLE IF NOT EXISTS "tag_hierarchy" (
	"parent_id" text NOT NULL REFERENCES "tags"("id") ON DELETE cascade,
	"child_id" text NOT NULL REFERENCES "tags"("id") ON DELETE cascade,
	CONSTRAINT "tag_hierarchy_parent_id_child_id_pk" PRIMARY KEY("parent_id","child_id")
);
CREATE TABLE IF NOT EXISTS "event_tags" (
	"event_id" text NOT NULL REFERENCES "events"("id") ON DELETE cascade,
	"tag_id" text NOT NULL REFERENCES "tags"("id") ON DELETE cascade,
	CONSTRAINT "event_tags_event_id_tag_id_pk" PRIMARY KEY("event_id","tag_id")
);
CREATE TABLE IF NOT EXISTS "event_meta" (
	"id" text PRIMARY KEY NOT NULL REFERENCES "events"("id") ON DELETE cascade,
	"updated_at" integer DEFAULT (strftime('%s', 'now') * 1000) NOT NULL,
	"source" blob NOT NULL,
	"format" blob NOT NULL
);
CREATE TABLE IF NOT EXISTS "event_cache" (
	"id" text PRIMARY KEY NOT NULL REFERENCES "events"("id") ON DELETE cascade,
	"updated_at" integer DEFAULT (strftime('%s', 'now') * 1000) NOT NULL,
	"raw" text,
	"parsed" blob,
	"revision" blob DEFAULT '{}' NOT NULL,
	"error" blob,
	"computed" blob DEFAULT '{}' NOT NULL
);

