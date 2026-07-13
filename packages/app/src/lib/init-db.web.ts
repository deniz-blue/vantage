import { schema, setDatabase } from "@vantage/db";
import { drizzle } from "drizzle-orm/sqlite-proxy";
import { SQLocalDrizzle } from "sqlocal/drizzle";

const sqlite = new SQLocalDrizzle(":localStorage:");

setDatabase(drizzle(sqlite.driver, sqlite.batchDriver, { schema, logger: true }));
