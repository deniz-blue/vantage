// Copy sqlite3.wasm alongside the export output so sqlite-wasm can find it.
// At runtime, sqlite-wasm resolves sqlite3.wasm relative to the page URL
// (the self.location.href fallback in our pnpm patch).

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

// Find sqlite-wasm in the pnpm store
const pnpmStore = path.resolve(root, "../../node_modules/.pnpm");
const dir = fs.readdirSync(pnpmStore)
	.find(d => d.startsWith("@sqlite.org+sqlite-wasm@"));

if (!dir) {
	console.error("Could not find @sqlite.org/sqlite-wasm in pnpm store");
	process.exit(1);
}

const src = path.join(
	pnpmStore, dir,
	"node_modules/@sqlite.org/sqlite-wasm/dist/sqlite3.wasm",
);
const dest = path.join(root, "dist", "sqlite3.wasm");
const destChunks = path.join(
	root, "dist", "_expo", "static", "js", "web", "sqlite3.wasm",
);

fs.cpSync(src, dest);
fs.mkdirSync(path.dirname(destChunks), { recursive: true });
fs.cpSync(src, destChunks);

console.log("✓ sqlite3.wasm copied for production export");
