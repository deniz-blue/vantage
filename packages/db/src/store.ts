import type { BaseSQLiteDatabase } from "drizzle-orm/sqlite-core/db";
import type * as schema from "./schema";

export type IVantageDrizzle = BaseSQLiteDatabase<"async" | "sync", any, typeof schema>;

export let db: IVantageDrizzle;

export const setDatabase = (database: IVantageDrizzle) => {
	db = database;
	console.log("<db variable set>");
};

Object.defineProperty(globalThis, "db", {
	get: () => {
		if (!db) throw new Error("Database not initialized");
		return db;
	},
});
