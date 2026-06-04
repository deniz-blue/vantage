import * as schema from "./schema";
import * as dz from "./drizzle-helpers";
import type { SqliteRemoteDatabase } from "drizzle-orm/sqlite-proxy";

export interface IVantageDrizzle extends SqliteRemoteDatabase<
	typeof schema
> { };

export let db: IVantageDrizzle;

export const setDatabase = (database: IVantageDrizzle) => {
	db = database;
};

export { schema, dz };
