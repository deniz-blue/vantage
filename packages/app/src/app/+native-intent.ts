const INTENT_PARAMS = ["at", "url", "data", "type"] as const;

const pickIntentParams = (path: string): string | null => {
	const url = new URL(path, "https://placeholder");

	if (url.pathname === "/e" || url.pathname === "/event") {
		const target = new URLSearchParams();
		for (const key of INTENT_PARAMS) {
			const val = url.searchParams.get(key);
			if (val) target.set(key, val);
		}
		if (target.size > 0) return `/event?${target.toString()}`;
	}

	if (url.pathname === "/" || url.pathname === "") {
		const hasIntent = INTENT_PARAMS.some((key) => url.searchParams.has(key));
		if (hasIntent) {
			const target = new URLSearchParams();
			for (const key of INTENT_PARAMS) {
				const val = url.searchParams.get(key);
				if (val) target.set(key, val);
			}
			return `/event?${target.toString()}`;
		}
	}

	return null;
};

export async function redirectSystemPath(intent: {
	path: string;
	initial: boolean;
}): Promise<string> {
	return pickIntentParams(intent.path) ?? intent.path;
}
