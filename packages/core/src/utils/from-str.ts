import { parseCanonicalResourceUri, type CanonicalResourceUri } from "@atcute/lexicons";
import { eventQueryFnNoId } from "../query/useEventQuery";
import { parseEventFormat } from "../lib/format";

export const eventQueryFnDataOrSourceStr = async ({
	data,
	source,
}: {
	data?: unknown;
	source?: string;
}): Promise<Vantage.ResolvedEvent> => {
	if (data) {
		const raw = JSON.stringify(data);
		const format: Vantage.EventFormat = { type: "directory.evnt.event" };
		const { parsed, error } = parseEventFormat(raw, format);
		return {
			id: null,
			data: parsed,
			raw,
			error,
			revision: {},
			source: { type: "unknown" },
			format,
		};
	}

	if (!source) throw new Error("Either source or data must be provided");

	return await eventQueryFnInferFromStr(source);
};

export const eventQueryFnInferFromStr = async (str: string) => {
	const { source, format } = await inferSourceFormat(str);
	return await eventQueryFnNoId(source, format);
};

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
		const { collection } = parseCanonicalResourceUri(str);
		if (collection !== "directory.evnt.event" && collection !== "community.lexicon.calendar.event") {
			throw new Error("Unsupported collection: " + collection);
		}
		return {
			format: { type: collection },
			source: { type: "at", uri: str as CanonicalResourceUri },
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
