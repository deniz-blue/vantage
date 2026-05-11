import { drizzle } from "drizzle-orm/sqlite-proxy";
import { schema } from "@vantage/db";
import initsql from "./migrations/init.sql?raw";
import { SQLocalDrizzle } from "sqlocal/drizzle";

export const sqlite = new SQLocalDrizzle("vantage-db.sqlite3");
export const db = drizzle(sqlite.driver, sqlite.batchDriver, {
	schema,
	logger: true,
});

console.log("Initializing database...");
await sqlite.transaction(async tx => {
	for (const stmt of initsql.split(";").map(s => s.trim()).map(s => s + ";")) {
		await tx.sql(stmt);
	}
});
console.log("Database initialized");
