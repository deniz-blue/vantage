// Uses :localStorage: (KVVFS driver) instead of a file path to avoid
// sqlocal's Worker-based OPFS driver, which requires the sqlite3.wasm
// files to be accessible alongside Worker bundles in production.
import { schema, setupDatabase } from "@vantage/db";
import { drizzle } from "drizzle-orm/sqlite-proxy";
import { SQLocalDrizzle } from "sqlocal/drizzle";

const sqlite = new SQLocalDrizzle(":localStorage:");

setupDatabase(drizzle(sqlite.driver, sqlite.batchDriver, { schema, logger: true }));
