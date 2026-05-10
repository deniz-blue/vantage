import { create } from "zustand";
import type { EventData } from "@evnt/schema";
import { PartialDateUtil, type PartialDate, type PlainDateString } from "@evnt/partial-date";
import { immer } from "zustand/middleware/immer";
import { enableMapSet } from "immer";

enableMapSet();

export interface CacheEventsStore {
	cache: {
		byText: Record<string, Set<Vantage.EventId>>;

		byPartialDate: Record<PartialDate, Set<Vantage.EventId>>;

		byWallDay: Record<PlainDateString, Set<Vantage.EventId>>;
		byWallMonth: Record<`${number}-${number}`, Set<Vantage.EventId>>; // "YYYY-MM"
	};

	hydrateSource: (id: Vantage.EventId) => Promise<void>;
	hydrate: (id: Vantage.EventId, data: EventData) => void;
	uncache: (id: Vantage.EventId) => void;
	init: () => Promise<void>;
};

export const useCacheEventsStore = create<CacheEventsStore>()(
	immer((set, get) => ({
		cache: {
			byPartialDate: {},
			byWallDay: {},
			byWallMonth: {},
			byText: {},
		},

		uncache: (id: Vantage.EventId) => set((state) => {
			for (const key in state.cache) {
				for (const entry in state.cache[key as keyof CacheEventsStore["cache"]]) {
					(state.cache as any)[key][entry].delete(id);
				}
			}
		}),

		hydrateSource: async (id: Vantage.EventId) => {
			// TODO
			get().uncache(id);
			// if (data?.data) {
			// 	get().hydrate(id, data.data);
			// }
		},

		hydrate: (id: Vantage.EventId, data: EventData) => set((state) => {
			// == text ==

			const text = [
				...Object.values(data.name),
				...Object.values(data.label ?? {}),
			].join(" ");
			state.cache.byText[text] ||= new Set();
			state.cache.byText[text].add(id);

			// == dates ==

			for (const instance of data.instances || []) {
				for (const key of ["start", "end"] as const) {
					const partialDate = instance[key];
					if (!partialDate) continue;

					state.cache.byPartialDate[partialDate] ||= new Set();
					state.cache.byPartialDate[partialDate].add(id);

					const parsed = PartialDateUtil.parse(partialDate);

					const pad = (num: number) => num.toString().padStart(2, "0");

					switch (parsed.precision) {
						case "time":
						case "day": {
							const dayKey = [parsed.year, parsed.month, parsed.day].map(pad).join("-") as PlainDateString;
							state.cache.byWallDay[dayKey] ||= new Set();
							state.cache.byWallDay[dayKey].add(id);
						} // Fallthrough intended
						case "month": {
							const monthKey = `${parsed.year}-${pad(parsed.month)}` as `${number}-${number}`;
							state.cache.byWallMonth[monthKey] ||= new Set();
							state.cache.byWallMonth[monthKey].add(id);
						} // Fallthrough intended
						default: break;
					}
				}
			}
		}),

		init: async () => {
			// TODO
		},
	}))
);
