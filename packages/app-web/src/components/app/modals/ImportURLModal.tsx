import { Button, Stack, TextInput } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { useState } from "react";
import { AsyncAction } from "../../data/AsyncAction";
import { parseCanonicalResourceUri } from "@atcute/lexicons";
import { dbShortcuts } from "../../../db/db-shortcuts";

export const ImportURLModal = ({
	context,
	id: modalId,
}: ContextModalProps<{}>) => {
	const [url, setUrl] = useState("");
	const [error, setError] = useState<string | null>(null);

	return (
		<Stack>
			<TextInput
				value={url}
				onChange={e => setUrl(e.currentTarget.value)}
				placeholder="URL to event JSON..."
				error={error}
				styles={{
					error: { whiteSpace: "pre-wrap" },
				}}
			/>
			<AsyncAction
				action={async () => {
					try {
						if (url.startsWith("at://")) {
							const p = parseCanonicalResourceUri(url);
							if (!p.ok) throw new Error("Invalid AT URI");
							if (p.value.collection !== "directory.evnt.event" && p.value.collection !== "community.lexicon.calendar.event") {
								throw new Error("Unsupported collection: " + p.value.collection);
							}
							await dbShortcuts.insertEventMeta({
								format: { type: p.value.collection },
								source: { type: "at", uri: url },
							});
						} else if (url.startsWith("http://") || url.startsWith("https://")) {
							const response = await fetch(url);
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
							await dbShortcuts.insertEventMeta({
								format,
								source: { type: "http", url },
							});
						} else {
							throw new Error("Unsupported URL scheme");
						}
						context.closeModal(modalId);
					} catch (error) {
						setError("" + error);
					}
				}}
			>
				{({ loading, onClick }) => (
					<Button
						disabled={!url}
						loading={loading}
						onClick={onClick}
					>
						Import
					</Button>
				)}
			</AsyncAction>
		</Stack>
	);
}
