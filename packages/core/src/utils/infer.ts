import { parseResourceUri, type CanonicalResourceUri } from "@atcute/lexicons";
import { EventResolver } from "../lib/resolve";
import { EventsLink } from "./eventslink";

type Matcher = (str: string) => Vantage.ResolvedEvent | null;

const matchAtUri: Matcher = (str) => {
	if (!str.startsWith("at://")) return null;

	const { repo, collection, rkey } = parseResourceUri(str);
	if (collection !== "directory.evnt.event" && collection !== "community.lexicon.calendar.event")
		return null;

	if (!rkey) return null;

	return EventResolver.new({
		format: { type: collection },
		source: { type: "at", uri: `at://${repo}/${collection}/${rkey}` as CanonicalResourceUri },
	});
};

const matchFolio: Matcher = (str) => {
	let url: URL;
	try {
		url = new URL(str);
	} catch {
		return null;
	}

	if (url.host !== "folio.denizblue.workers.dev") return null;

	const parts = url.pathname.split("/").filter(Boolean);
	if (parts[0] !== "events" || !parts[1]) return null;

	return EventResolver.new({
		source: {
			type: "folio",
			id: parts[1],
			baseUrl: url.origin,
			editToken: url.searchParams.get("token") ?? undefined,
		},
		format: { type: "directory.evnt.event" },
	});
};

const matchMediaWiki: Matcher = (str) => {
	const regex = /^https?:\/\/(.*)\/rest.php\/v1\/page\/(.*)$/;
	const match = str.match(regex);
	if (!match) return null;

	return EventResolver.new({
		source: { type: "mediawiki", url: `https://${match[1]}/`, title: decodeURIComponent(match[2]!) },
		format: { type: "directory.evnt.event" },
	});
};

const matchEventsLink: Matcher = (str) => {
	if (!str.startsWith("https://eventsl.ink/")) return null;

	let url: URL;
	try {
		url = new URL(str);
	} catch {
		return null;
	}

	const intent = EventsLink.parseIntent(url);
	if (intent?.type !== "event") return null;

	if (intent.at) return matchAtUri(intent.at);
	if (intent.url) return matchHttpUrl(intent.url);
	if (intent.data)
		return EventResolver.new({
			format: { type: "directory.evnt.event" },
			source: { type: "unknown" },
			raw: intent.data,
		});

	return null;
};

const matchHttpUrl: Matcher = (str) => {
	let url: URL;
	try {
		url = new URL(str);
	} catch {
		return null;
	}

	if (url.protocol !== "http:" && url.protocol !== "https:") return null;

	let format: Vantage.EventFormat = { type: "unknown" };
	if (str.endsWith(".evnt.json") || str.endsWith(".json")) format = { type: "directory.evnt.event" };
	if (str.endsWith(".ics")) format = { type: "ics" };

	return EventResolver.new({
		source: { type: "http", url: str },
		format,
	});
};

const matchers: Matcher[] = [
	matchAtUri,
	matchFolio,
	matchMediaWiki,
	matchEventsLink,
	matchHttpUrl,
];

export const Infer = {
	fromString: (str: string): Vantage.ResolvedEvent => {
		for (const matcher of matchers) {
			const result = matcher(str);
			if (result) return result;
		}

		return EventResolver.new();
	},
};
