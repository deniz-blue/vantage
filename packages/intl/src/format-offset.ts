// Uses Intl.DateTimeFormat which is available on Hermes (bundles ICU4C).
// No polyfill needed — this is the one Intl API that actually works in RN.

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
export const offsetNumeric = (tz: string): number => computeOffset(tz).numeric;
