import type { EventStatus } from "@evnt/types";

// === Source badge config ===

export interface BadgeDef {
	color: string;
	label: string;
}

export const sourceBadge = {
	local: { color: "Dark2", label: "L" },
	http: { color: "Green", label: "H" },
	at: { color: "Blue", label: "@" },
	mediawiki: { color: "Purple", label: "W" },
	webdav: { color: "Orange", label: "W" },
} satisfies Record<string, BadgeDef>;

export const sourceBadgeFallback: BadgeDef = { color: "Gray5", label: "?" };

// === Format badge config ===

export const formatBadge = {
	ics: { color: "Orange", label: "ICS" },
	"community.lexicon.calendar.event": { color: "Cyan", label: "C" },
	"directory.evnt.event": { color: "Grape6", label: "E" },
} satisfies Record<string, BadgeDef>;

export const formatBadgeFallback: BadgeDef = { color: "Amber", label: "?" };

// === Status color lookup ===

export const statusBadgeColor = (status: EventStatus): string => {
	switch (status) {
		case "cancelled":
			return "Red";
		case "postponed":
			return "Orange";
		case "uncertain":
			return "Yellow";
		case "suspended":
			return "Grey";
		default:
			return "Dark2";
	}
};
