import type { BaseSQLiteDatabase } from "drizzle-orm/sqlite-core/db";
import type * as schema from "./schema";

// BaseSQLiteDatabase with union of both adapter type params.
// Both SqliteRemoteDatabase (async) and ExpoSQLiteDatabase (sync) extend this.
export type IVantageDrizzle = BaseSQLiteDatabase<
	"async" | "sync",
	any,
	typeof schema
>;

export let db: IVantageDrizzle;

export const setDatabase = (database: IVantageDrizzle) => {
	db = database;
};
