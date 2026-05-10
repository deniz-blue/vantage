CREATE TABLE IF NOT EXISTS "event_cache" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"updated_at" timestamp DEFAULT now() NOT NULL,
	"raw" text,
	"parsed" jsonb,
	"revision" jsonb NOT NULL,
	"error" jsonb
);
CREATE TABLE IF NOT EXISTS "event_meta" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"updated_at" timestamp DEFAULT now() NOT NULL,
	"source" jsonb NOT NULL,
	"format" jsonb NOT NULL
);
CREATE TABLE IF NOT EXISTS "event_tags" (
	"event_id" uuid NOT NULL,
	"tag_id" uuid NOT NULL,
	CONSTRAINT "event_tags_event_id_tag_id_pk" PRIMARY KEY("event_id","tag_id")
);
CREATE TABLE IF NOT EXISTS "events" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"updated_at" timestamp DEFAULT now() NOT NULL
);
CREATE TABLE IF NOT EXISTS "tag_hierarchy" (
	"parent_id" uuid NOT NULL,
	"child_id" uuid NOT NULL,
	CONSTRAINT "tag_hierarchy_parent_id_child_id_pk" PRIMARY KEY("parent_id","child_id")
);
CREATE TABLE IF NOT EXISTS "tags" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"name" text NOT NULL,
	"color" text,
	"updated_at" timestamp DEFAULT now() NOT NULL
);
ALTER TABLE "event_cache" DROP CONSTRAINT IF EXISTS "event_cache_id_events_id_fk";
ALTER TABLE "event_meta" DROP CONSTRAINT IF EXISTS "event_meta_id_events_id_fk";
ALTER TABLE "event_tags" DROP CONSTRAINT IF EXISTS "event_tags_event_id_events_id_fk";
ALTER TABLE "event_tags" DROP CONSTRAINT IF EXISTS "event_tags_tag_id_tags_id_fk";
ALTER TABLE "tag_hierarchy" DROP CONSTRAINT IF EXISTS "tag_hierarchy_parent_id_tags_id_fk";
ALTER TABLE "tag_hierarchy" DROP CONSTRAINT IF EXISTS "tag_hierarchy_child_id_tags_id_fk";
ALTER TABLE "event_cache" ADD CONSTRAINT "event_cache_id_events_id_fk" FOREIGN KEY ("id") REFERENCES "public"."events"("id") ON DELETE cascade ON UPDATE no action;
ALTER TABLE "event_meta" ADD CONSTRAINT "event_meta_id_events_id_fk" FOREIGN KEY ("id") REFERENCES "public"."events"("id") ON DELETE cascade ON UPDATE no action;
ALTER TABLE "event_tags" ADD CONSTRAINT "event_tags_event_id_events_id_fk" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE cascade ON UPDATE no action;
ALTER TABLE "event_tags" ADD CONSTRAINT "event_tags_tag_id_tags_id_fk" FOREIGN KEY ("tag_id") REFERENCES "public"."tags"("id") ON DELETE cascade ON UPDATE no action;
ALTER TABLE "tag_hierarchy" ADD CONSTRAINT "tag_hierarchy_parent_id_tags_id_fk" FOREIGN KEY ("parent_id") REFERENCES "public"."tags"("id") ON DELETE cascade ON UPDATE no action;
ALTER TABLE "tag_hierarchy" ADD CONSTRAINT "tag_hierarchy_child_id_tags_id_fk" FOREIGN KEY ("child_id") REFERENCES "public"."tags"("id") ON DELETE cascade ON UPDATE no action;

