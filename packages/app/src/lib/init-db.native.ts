import { schema, setupDatabase } from "@vantage/db";
import { drizzle } from "drizzle-orm/expo-sqlite";
import { openDatabaseSync } from "expo-sqlite";

const expo = openDatabaseSync("vantage.db");

setupDatabase(drizzle(expo, { schema, logger: true }));
