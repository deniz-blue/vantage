import { parseResourceUri, type CanonicalResourceUri } from "@atcute/lexicons";
import { isDid, isHandle } from "@atcute/lexicons/syntax";
import { handleResolver } from "@vantage/atproto";
import { EventResolver } from "../lib/resolve";
import { EventsLink } from "./eventslink";

export const FromStr = {
	infer: async (str: string): Promise<Vantage.ResolvedEvent> => {
		if (str.startsWith("https://eventsl.ink/")) {
			const intent = EventsLink.parseIntent(new URL(str));
			if (intent?.type === "event") {
				if (intent.at) return FromStr.inferAtUri(intent.at);
				if (intent.url) return FromStr.inferHttpUrl(intent.url);
				if (intent.data)
					return EventResolver.new({
						format: { type: "directory.evnt.event" },
						source: { type: "unknown" },
						raw: intent.data,
					});
			}
		}

		const mw = await FromStr.tryInferMediawiki(str);
		if (mw) return mw;

		if (str.startsWith("at://")) {
			return FromStr.inferAtUri(str);
		} else if (str.startsWith("http://") || str.startsWith("https://")) {
			return FromStr.inferHttpUrl(str);
		} else {
			throw new Error("Unsupported URL scheme");
		}
	},

	inferAtUri: async (uri: string): Promise<Vantage.ResolvedEvent> => {
		const { repo, collection, rkey } = parseResourceUri(uri);
		if (collection !== "directory.evnt.event" && collection !== "community.lexicon.calendar.event")
			throw new Error("Unsupported collection: " + collection);

		if (!rkey) throw new Error("Missing rkey in at URI");

		let did = repo;
		if (!isDid(repo) && isHandle(repo)) {
			did = await handleResolver.resolve(repo);
		}

		return EventResolver.new({
			format: { type: collection },
			source: { type: "at", uri: `at://${did}/${collection}/${rkey}` as CanonicalResourceUri },
		});
	},

	tryInferMediawiki: async (url: string): Promise<Vantage.ResolvedEvent | null> => {
		const mediawikiRegex = /^https?:\/\/(.*)\/rest.php\/v1\/page\/(.*)$/;
		if (mediawikiRegex.test(url)) {
			const match = url.match(mediawikiRegex);
			if (!match) throw new Error("Invalid MediaWiki URL");
			const wikiUrl = `https://${match[1]}/`;
			const title = decodeURIComponent(match[2]!);

			return EventResolver.new({
				format: { type: "directory.evnt.event" },
				source: { type: "mediawiki", url: wikiUrl, title },
			});
		}

		return null;
	},

	inferHttpUrl: async (url: string): Promise<Vantage.ResolvedEvent> => {
		let source: Vantage.EventSource = { type: "http", url };
		let format: Vantage.EventFormat = { type: "unknown" };

		if (url.endsWith(".evnt.json")) format = { type: "directory.evnt.event" };
		if (url.endsWith(".json")) format = { type: "directory.evnt.event" };
		if (url.endsWith(".ics")) format = { type: "ics" };

		const probe = async () => {
			const response = await fetch(url, { method: "HEAD" });
			if (!response.ok) throw new Error("Failed to fetch URL: " + response.statusText);

			const contentType = response.headers.get("content-type") ?? "";
			if (contentType.includes("application/json")) {
				const data = await response.json();
				if (data && typeof data === "object" && "$type" in data && typeof data.$type === "string") {
					if (
						data.$type === "directory.evnt.event" ||
						data.$type === "community.lexicon.calendar.event"
					) {
						format = { type: data.$type };
					}
				} else if (data && typeof data === "object" && "v" in data && "name" in data) {
					format = { type: "directory.evnt.event" };
				}
			} else if (contentType.includes("text/calendar") || url.endsWith(".ics")) {
				format = { type: "ics" };
			}
		};

		if (format.type === "unknown") await probe();

		return EventResolver.new({
			format,
			source,
		});
	},
};
