import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { schema } from "@vantage/db";
import initsql from "./migrations/init.sql?raw";

const client = new PGlite("idb://vantage-pglite");
export const db = drizzle(client, {
	schema,
	logger: true,
});

console.log("Initializing database...");
await client.transaction(async tx => {
	for (const stmt of initsql.split(";").map(s => s.trim()).map(s => s + ";")) {
		await tx.exec(stmt);
	}
});
console.log("Database initialized");
