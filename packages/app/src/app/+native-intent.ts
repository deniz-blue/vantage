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
		if (url.searchParams.has("type") && url.searchParams.get("type") === "event") {
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
	console.log("redirectSystemPath", intent);
	return pickIntentParams(intent.path) ?? intent.path;
}
