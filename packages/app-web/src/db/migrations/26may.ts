import { openDB } from "idb";
import { dbShortcuts } from "../db-shortcuts";
import { parseResourceUri } from "@atcute/lexicons";
import { notifications } from "@mantine/notifications";

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

	const id = "migration-26may";
	notifications.show({
		title: "Migration",
		message: "Migrating database, please wait...",
		id,
		loading: true,
		autoClose: false,
		withCloseButton: false,
	});

	for (const src of srclist) {
		const [type, data] = src.split("://");
		switch (type) {
			case "local": {
				const row: { data: any } = await db.get("data", src);
				console.log(JSON.stringify(row.data), inferEventFormat(row.data));
				const fmt = inferEventFormat(row.data);
				const id = await dbShortcuts.insertLocalEvent(
					fmt.type == "ics" ? row.data.value : JSON.stringify(row.data),
					fmt
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

		notifications.update({
			id,
			title: "Migration",
			message: `Migrating database, please wait... (${createdIds.length}/${srclist.length})`,
			loading: true,
			autoClose: false,
			withCloseButton: false,
		});
	}

	notifications.update({
		id,
		title: "Migration complete",
		message: `Migration complete! ${createdIds.length}/${srclist.length} events have been migrated.`,
		loading: false,
		autoClose: 5000,
		withCloseButton: true,
	});
};
