import "dotenv/config";
import app from "./index";
import { serve } from "@hono/node-server";

const { PORT = 3000 } = process.env;

serve({
	fetch: app.fetch,
	port: Number(PORT),
});
