export const tryCatch = <T>(fn: () => T, catchFn: (e: unknown) => T): T => {
	try {
		return fn();
	} catch (e) {
		return catchFn(e);
	}
};

export const trynull = <T>(fn: () => T): T | null => tryCatch(fn, () => null);
