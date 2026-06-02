import { BaseSQLiteDatabase } from "drizzle-orm/sqlite-core";
import * as schema from "./schema";
import * as dz from "./drizzle-helpers";

export interface IVantageDrizzle extends BaseSQLiteDatabase<
	"async",
	{ rows?: unknown[] },
	typeof schema
> { };

export let db: IVantageDrizzle;

export const setDatabase = (database: IVantageDrizzle) => {
	db = database;
};

export { schema, dz };
