import type { EventData } from "@evnt/schema";
import { PartialDateUtil } from "@evnt/partial-date";

export const createComputedData = (parsed?: EventData | null): Vantage.ComputedData => {
	if (!parsed) return {};

	const timeRanges: { low: number; high: number }[] = [];

	for (const instance of parsed.instances ?? []) {
		if (!instance.start) continue;
		const low = PartialDateUtil.toInstant(instance.start, "low");
		const high = instance.end
			? PartialDateUtil.toInstant(instance.end, "high")
			: PartialDateUtil.toInstant(instance.start, "high");
		timeRanges.push({
			low: low.epochMilliseconds,
			high: high.epochMilliseconds,
		});
	}

	return {
		timeRanges,
	};
};
