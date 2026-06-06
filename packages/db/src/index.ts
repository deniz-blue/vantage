import * as schema from "./schema";
import * as dz from "./drizzle-helpers";
import type { SqliteRemoteDatabase } from "drizzle-orm/sqlite-proxy";
import type { ExpoSQLiteDatabase } from "drizzle-orm/expo-sqlite";

export type IVantageDrizzle = SqliteRemoteDatabase<typeof schema> | ExpoSQLiteDatabase<typeof schema>;

export let db: IVantageDrizzle;

export const setDatabase = (database: IVantageDrizzle) => {
	db = database;
};

export { schema, dz };
