// === Offset computation — module-level cache ===

const offsetCache = new Map<string, { numeric: number; display: string }>();

const computeOffset = (tz: string): { numeric: number; display: string } => {
	const cached = offsetCache.get(tz);
	if (cached) return cached;

	try {
		const now = Date.now();
		const formatter = new Intl.DateTimeFormat("en", {
			timeZone: tz,
			timeZoneName: "shortOffset",
		});
		const parts = formatter.formatToParts(now);
		const raw = parts.find((p) => p.type === "timeZoneName")?.value || "";

		const sign = raw.startsWith("GMT-") ? -1 : 1;
		const cleaned = raw.replace(/^GMT[+-]/, "");
		const [h, m] = cleaned.split(":");
		const hours = Number(h) || 0;
		const minutes = Number(m) || 0;
		const numeric = sign * (hours * 60 + minutes);
		const display = raw.replace(/^GMT/, "UTC");
		const result = { numeric, display };
		offsetCache.set(tz, result);
		return result;
	} catch {
		const result = { numeric: 9999, display: "" };
		offsetCache.set(tz, result);
		return result;
	}
};

export const formatOffset = (tz: string): string => computeOffset(tz).display;

// === Sorted & grouped timezone data (module-level) ===

export const allTimezones: readonly string[] = Intl.supportedValuesOf("timeZone")
	.map((tz) => ({ tz, offset: computeOffset(tz).numeric }))
	.sort((a, b) => a.offset - b.offset || a.tz.localeCompare(b.tz))
	.map(({ tz }) => tz);

export const allRegions: readonly string[] = Array.from(
	new Set(
		allTimezones.map((tz) =>
			tz.includes("/") ? tz.split("/")[0]! : "Other",
		),
	),
).sort((a, b) => a.localeCompare(b));

export const timezonesInRegion = (region: string): readonly string[] =>
	region === "Other"
		? allTimezones.filter((tz) => !tz.includes("/"))
		: allTimezones.filter((tz) => tz.startsWith(region + "/"));

export const getDetectedTz = (): string => {
	try {
		return Intl.DateTimeFormat().resolvedOptions().timeZone;
	} catch {
		return "UTC";
	}
};
