import { PGlite } from "@electric-sql/pglite";
import { PGliteWorker } from "@electric-sql/pglite/worker";
import { drizzle } from "drizzle-orm/pglite";
import { schema } from "@vantage/db";
import initsql from "./migrations/init.sql?raw";
import pgLiteWorker from "./pglite.worker?worker";

const client = new PGliteWorker(new pgLiteWorker());
export const db = drizzle(client as any as PGlite, {
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
