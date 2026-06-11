import { setDatabase, type IVantageDrizzle } from "./store";

export const setupDatabase = (database: IVantageDrizzle) => {
	setDatabase(database);
};
