import { dz } from "@vantage/db";
import { sqliteTable } from "drizzle-orm/sqlite-core";
import { drizzle } from "drizzle-orm/better-sqlite3";
import Database from "better-sqlite3";

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

export { db, sync1 };
