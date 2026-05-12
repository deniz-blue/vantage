import { parseCanonicalResourceUri } from "@atcute/lexicons";

export const inferSourceFormat = async (str: string): Promise<{
	source: Vantage.EventSource;
	format: Vantage.EventFormat;
}> => {
	const mediawikiRegex = /^https?:\/\/(.*)\/rest.php\/v1\/page\/(.*)$/;
	if (mediawikiRegex.test(str)) {
		const match = str.match(mediawikiRegex);
		if (!match) throw new Error("Invalid MediaWiki URL");
		const wikiUrl = `https://${match[1]}/`;
		const title = decodeURIComponent(match[2]!);

		return {
			format: { type: "directory.evnt.event" },
			source: { type: "mediawiki", url: wikiUrl, title },
		};
	}

	if (str.startsWith("at://")) {
		const p = parseCanonicalResourceUri(str);
		if (!p.ok) throw new Error("Invalid AT URI");
		if (p.value.collection !== "directory.evnt.event" && p.value.collection !== "community.lexicon.calendar.event") {
			throw new Error("Unsupported collection: " + p.value.collection);
		}
		return {
			format: { type: p.value.collection },
			source: { type: "at", uri: str },
		};
	} else if (str.startsWith("http://") || str.startsWith("https://")) {
		let source: Vantage.EventSource = { type: "http", url: str };
		const response = await fetch(str);
		if (!response.ok) throw new Error("Failed to fetch URL: " + response.statusText);
		let format: Vantage.EventFormat = { type: "unknown" } as const;
		const contentType = response.headers.get("content-type") ?? "";
		if (contentType.includes("application/json")) {
			const data = await response.json();
			if (data && typeof data === "object" && "$type" in data && typeof data.$type === "string") {
				if (data.$type === "directory.evnt.event" || data.$type === "community.lexicon.calendar.event") {
					format = { type: data.$type };
				}
			}
		} else if (contentType.includes("text/calendar")) {
			format = { type: "ics" };
		}
		return {
			format,
			source,
		};
	} else {
		throw new Error("Unsupported URL scheme");
	}
};
