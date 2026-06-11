// Metro resolves db-init.native.ts or db-init.web.ts at build time.
// This stub exists so TypeScript sees the import signature.
export const initDb: () => Promise<void> = async () => {
	throw new Error("Platform-specific db-init not loaded");
};
