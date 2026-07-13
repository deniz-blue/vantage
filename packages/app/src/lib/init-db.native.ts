import { schema, setDatabase } from "@vantage/db";
import { drizzle } from "drizzle-orm/expo-sqlite";
import { openDatabaseSync } from "expo-sqlite";

console.log("Using expo-sqlite for the database");

const expo = openDatabaseSync("vantage.db");
setDatabase(drizzle(expo, { schema, logger: true }));
