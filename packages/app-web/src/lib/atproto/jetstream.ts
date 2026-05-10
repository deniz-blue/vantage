import { JetstreamSubscription, type CommitEvent } from "@atcute/jetstream";
import { Logger } from "../util/logger";
import { $NSID } from "@evnt/schema";
import { CanonicalResourceUri } from "@atcute/lexicons";
import { AtprotoDid } from "@atcute/lexicons/syntax";

const JetstreamLogger = Logger.main.styledChild("Jetstream", "#88c0d0");

const jetstreamCursorKey = "vantage:jetstreamCursor";

export const createJetstream = ({
	onUpdate,
}: {
	onUpdate?: (source: CanonicalResourceUri, record: unknown, event: CommitEvent) => void;
}) => {
	let unmounted = false;
	const subscription = new JetstreamSubscription({
		url: "wss://jetstream2.us-east.bsky.network",
		wantedCollections: [$NSID],
		cursor: Number(localStorage.getItem(jetstreamCursorKey)) || undefined,
		onConnectionOpen: () => JetstreamLogger.log("Connection opened"),
		onConnectionClose: () => JetstreamLogger.log("Connection closed"),
		onConnectionError: (error) => JetstreamLogger.log("Connection error:", error),
	});

	JetstreamLogger.log("Initialized");

	(async () => {
		for await (const event of subscription) {
			if (unmounted) break;
			if (event.kind == "commit" && event.commit.collection === $NSID) {
				JetstreamLogger.log("Commit:", event);
				const aturi: CanonicalResourceUri = `at://${event.did as AtprotoDid}/${event.commit.collection}/${event.commit.rkey}`;
				JetstreamLogger.log(event.commit.operation + ":", aturi);

				if (event.commit.operation !== "delete") onUpdate?.(aturi, event.commit.record, event);
			}
		}
	})();

	const interval = setInterval(() => {
		if (subscription.cursor !== undefined) {
			localStorage.setItem(jetstreamCursorKey, String(subscription.cursor));
		}
	}, 5_000);

	return () => {
		unmounted = true;
		clearInterval(interval);
	};
};
