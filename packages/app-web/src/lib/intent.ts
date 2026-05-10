import { isCanonicalResourceUri, parseCanonicalResourceUri } from "@atcute/lexicons";
import z from "zod";

export const AtUriSchema = z.string().refine(s => isCanonicalResourceUri(s), { message: "Invalid at URI" });
export const HttpUrlSchema = z.url({ protocol: /^(https?)$/ });

export const RemoteUriSchema = z.union([AtUriSchema, HttpUrlSchema]);
export type RemoteUri = z.infer<typeof RemoteUriSchema>;

export const remoteUriToSourceFormat = (uri: RemoteUri): {
	source: Vantage.EventSource;
	format: Vantage.EventFormat;
} => {
	let source: Vantage.EventSource = { type: "unknown" };
	let format: Vantage.EventFormat = { type: "directory.evnt.event" };

	if (uri.startsWith("http")) {
		source = { type: "http", url: uri };
		if (uri.endsWith(".ics")) format = { type: "ics" };
	}

	if (uri.startsWith("at")) {
		source = { type: "at", uri: uri };
		const p = parseCanonicalResourceUri(uri);
		if (p.ok) {
			const collection = p.value.collection;
			if (collection === "community.lexicon.calendar.event") format = { type: "community.lexicon.calendar.event" };
		}
	}

	return { source, format };
};
