import { schema, setupDatabase } from "@vantage/db";
import { drizzle } from "drizzle-orm/expo-sqlite";
import { openDatabaseSync } from "expo-sqlite";

export const initDb = async () => {
	const expo = openDatabaseSync("vantage.db");
	const database = drizzle(expo, { schema, logger: true });

	setupDatabase(database);
};
