import { Platform } from "react-native";
import { setDatabase, schema } from "@vantage/db";

export const initDb = async () => {
	if (Platform.OS === "web") {
		const { SQLocalDrizzle } = await import("sqlocal/drizzle");
		const { drizzle } = await import("drizzle-orm/sqlite-proxy");
		const sqlite = new SQLocalDrizzle("idb://vantage-db.sqlite3");
		const db = drizzle(sqlite.driver, sqlite.batchDriver, {
			schema,
			logger: true,
		});
		setDatabase(db);
	} else {
		const { drizzle } = await import("drizzle-orm/expo-sqlite");
		const { openDatabaseSync } = await import("expo-sqlite");
		const expo = openDatabaseSync("vantage.db");
		const db = drizzle(expo, {
			schema,
			logger: true,
		});
		setDatabase(db);
	}
};
