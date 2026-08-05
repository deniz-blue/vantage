import { EventsLink } from "@vantage/core";

export async function redirectSystemPath(intent: {
	path: string;
	initial: boolean;
}): Promise<string> {
	const url = new URL(intent.path, "https://placeholder");

	if (url.pathname === "/oauth/callback" || url.pathname.startsWith("/oauth/callback/")) {
		const params = url.searchParams.toString();
		return params ? `/oauth/callback?${params}` : intent.path;
	}

	const intentData = EventsLink.parseIntent(url);
	if (intentData) {
		const target = new URLSearchParams();
		for (const [key, value] of Object.entries(intentData)) {
			if (value) target.set(key, value);
		}
		if (target.size > 0) return `/event?${target.toString()}`;
	}

	return intent.path;
}
