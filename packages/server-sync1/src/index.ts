import { Hono } from "hono";
import { basicAuth } from "hono/basic-auth";
import { drizzle } from "drizzle-orm/better-sqlite3";
import Database from "better-sqlite3";
import { zValidator } from "@hono/zod-validator";
import { sqliteTable,  } from "drizzle-orm/sqlite-core";
import { dz } from "@vantage/db";
import { z } from "zod";
import { eq } from "drizzle-orm";

const sync1 = sqliteTable("sync1", {
	id: dz.uuid("id").primaryKey(),
	data: dz.jsonb("data").notNull(),
	updatedAt: dz.timestamp("updated_at").notNull(),
	deleted: dz.bool("deleted").notNull().default(false),
});

const sqlite = new Database("sync.db");
const db = drizzle(sqlite, { schema: { sync1 } });

sqlite.exec(`
CREATE TABLE IF NOT EXISTS sync1 (
	id TEXT PRIMARY KEY,
	data BLOB NOT NULL,
	updated_at INTEGER NOT NULL,
	deleted INTEGER NOT NULL DEFAULT 0
);
`);

const app = new Hono<{ Variables: { SECRET_KEY: string } }>()

app.get("/", (c) => {
	return c.json({ message: "Sync1 Server running" });
});

app.use("/1/*", async (c, next) => {
	const auth = basicAuth({
		username: "root",
		password: c.var.SECRET_KEY,
	});

	return auth(c, next)
});

app.get("/1/snapshot", async (c) => {
	const rows = await db
		.select()
		.from(sync1)
		.where(eq(sync1.deleted, false));
	return c.json(rows);
});

const SyncDeltaSchema = z.object({
	id: z.uuid(),
	data: z.union([z.looseObject({}), z.null()]),
	updatedAt: z.number().transform((val) => Temporal.Instant.fromEpochMilliseconds(val)),
});

app.post("/1/sync", zValidator("json", SyncDeltaSchema.array()), async (c) => {
	const deltas = c.req.valid("json");

	const deleteDeltas = deltas.filter((delta) => delta.data === null);
	const upsertDeltas = deltas.filter((delta) => delta.data !== null);

	for (const delta of deleteDeltas) {
		await db
			.update(sync1)
			.set({ deleted: true })
			.where(eq(sync1.id, delta.id));
	}

	for (const delta of upsertDeltas) {
		await db
			.insert(sync1)
			.values({
				id: delta.id,
				data: delta.data,
				updatedAt: delta.updatedAt,
				deleted: false,
			})
			.onConflictDoUpdate({
				target: sync1.id,
				set: {
					data: delta.data,
					updatedAt: delta.updatedAt,
					deleted: false,
				},
			});
	}

	return c.json({ message: "Sync successful" });
});

export default app;
