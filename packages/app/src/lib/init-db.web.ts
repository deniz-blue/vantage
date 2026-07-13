import { schema, setDatabase } from "@vantage/db";
import { drizzle } from "drizzle-orm/sqlite-proxy";
import { SQLocalDrizzle } from "sqlocal/drizzle";

console.log("Using sqlocal for the database");

const sqlite = new SQLocalDrizzle(":localStorage:");
setDatabase(drizzle(sqlite.driver, sqlite.batchDriver, { schema, logger: true }));
