import { PGlite } from "@electric-sql/pglite";
import { drizzle } from "drizzle-orm/pglite";
import { schema } from "@vantage/db";

const client = new PGlite("idb://vantage-pglite");
export const db = drizzle(client, { schema });
