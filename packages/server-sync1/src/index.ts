import { Hono } from "hono";
import { basicAuth } from "hono/basic-auth";
import { zValidator } from "@hono/zod-validator";
import { z } from "zod";
import { eq } from "drizzle-orm";
import { logger } from "hono/logger";
import { db, sync1 } from "./db";

const app = new Hono<{ Variables: { SECRET_KEY: string } }>()

app.use(logger());

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
