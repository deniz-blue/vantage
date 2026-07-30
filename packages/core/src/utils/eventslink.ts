import type { CanonicalResourceUri } from "@atcute/lexicons";

export interface Intent {
	type: "event";
	at?: string;
	url?: string;
	data?: string;
}

export const EventsLink = new (class {
	BASE_URL = "https://eventsl.ink/";

	parseIntent(url: URL): Intent | null {
		if (
			url.pathname === "/e" ||
			url.pathname === "/event" ||
			url.searchParams.get("type") === "event"
		) {
			const intent: Intent = { type: "event" };
			if (url.searchParams.has("at")) intent.at = url.searchParams.get("at")!;
			if (url.searchParams.has("url")) intent.url = url.searchParams.get("url")!;
			if (url.searchParams.has("data")) intent.data = url.searchParams.get("data")!;
			return intent;
		}

		return null;
	}

	intentSource(intent: Intent): Vantage.EventSource | null {
		if (intent.at) return { type: "at", uri: intent.at as CanonicalResourceUri };
		if (intent.url) return { type: "http", url: intent.url };
		if (intent.data) return { type: "unknown" };
		return null;
	}
})();
