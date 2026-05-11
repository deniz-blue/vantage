import { PGlite } from "@electric-sql/pglite";
import { worker } from "@electric-sql/pglite/worker";

worker({
	init: async () => new PGlite("idb://vantage-pglite"),
});
