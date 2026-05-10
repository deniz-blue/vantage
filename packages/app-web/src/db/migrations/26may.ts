import { openDB } from "idb";
import { dbShortcuts } from "../db-shortcuts";
import { parseResourceUri } from "@atcute/lexicons";

type EventSource = string;

// Key `event-app:userdata`
interface LocalStorageUserData {
	state?: {
		layers: Record<string, {
			data: {
				events: EventSource[];
			};
		}>;
	};
	version: 2;
}

export const _migrate26may_ = async () => {
	const ud = JSON.parse(localStorage.getItem('event-app:userdata') ?? '{}') as LocalStorageUserData;
	const srclist = ud.state?.layers.default?.data.events ?? [];

	const createdIds: string[] = [];

	const db = await openDB("event-app:events-db", 10);

	const inferEventFormat = (data: unknown): Vantage.EventFormat => {
		if (!data || typeof data !== "object") return { type: "unknown" };
		if (
			"$type" in data
			&& typeof data.$type === "string"
			&& (
				data.$type === "directory.evnt.event"
				|| data.$type === "community.lexicon.calendar.event"
			)
		) return { type: data.$type };
		if ("$type" in data && data.$type === "com.apple.icalendar") return { type: "ics" };
		const obj = data as Record<string, unknown>;
		if ("v" in obj && "name" in obj) return { type: "directory.evnt.event" };
		return { type: "unknown" };
	};

	for (const src of srclist) {
		const [type, data] = src.split("://");
		switch (type) {
			case "local": {
				const row: { data: any } = await db.get("data", src);
				const id = await dbShortcuts.insertLocalEvent(
					JSON.stringify(row.data),
					inferEventFormat(row.data)
				);
				createdIds.push(id);
			} break;
			case "at": {
				const p = parseResourceUri(src);
				if (!p.ok) continue;
				let format: Vantage.EventFormat = { type: "unknown" };
				if (p.value.collection === "directory.evnt.event") format = { type: "directory.evnt.event" };
				if (p.value.collection === "community.lexicon.calendar.event") format = { type: "community.lexicon.calendar.event" };
				const id = await dbShortcuts.insertEventMeta({
					source: { type: "at", uri: src },
					format,
				});
				createdIds.push(id);
			} break;
			case "https":
			case "http": {
				// what could go wrong? -deniz
				const row: { data: any } = await db.get("data", src);
				const format = inferEventFormat(row.data);
				const id = await dbShortcuts.insertEventMeta({
					source: { type: "http", url: src },
					format,
				});
				createdIds.push(id);
			} break;
		}
	}
};
