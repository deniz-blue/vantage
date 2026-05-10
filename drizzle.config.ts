import { defineConfig } from "drizzle-kit";

export default defineConfig({
	out: "./packages/db/src/migrations",
	schema: "./packages/db/src/schema.ts",
	dialect: "postgresql",
});
