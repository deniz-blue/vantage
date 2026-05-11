import type { EventData, LanguageKey, PartialDate } from "@evnt/schema";
import { TranslationsUtil } from "@evnt/translations";
import { EventQuery } from "@vantage/core";

export type EventSorter = (a: EventData, b: EventData) => number;

export const EventSorters = {
	None: () => 0,

	Invert: (sorter: EventSorter): EventSorter => (a, b) => -sorter(a, b),

	Name: (userLanguage: LanguageKey) => (a: EventData, b: EventData) => {
		const t = TranslationsUtil.createTranslator([userLanguage]);
		return t(a.name).localeCompare(t(b.name), userLanguage);
	},

	InstanceStart: (a: EventData, b: EventData) => {
		const aStartDates = a.instances?.map(instance => instance.start).filter((date): date is PartialDate => !!date) || [];
		const bStartDates = b.instances?.map(instance => instance.start).filter((date): date is PartialDate => !!date) || [];

		const aMinStart = aStartDates.reduce((min, date) => (!min || date < min) ? date : min, aStartDates[0]);
		const bMinStart = bStartDates.reduce((min, date) => (!min || date < min) ? date : min, bStartDates[0]);

		if (!aMinStart && !bMinStart) return 0;
		if (!aMinStart) return 1;
		if (!bMinStart) return -1;

		return aMinStart > bMinStart ? 1 : aMinStart < bMinStart ? -1 : 0;
	},
} as const;

export const applyEventSorters = (queries: EventQuery[], sorters: EventSorter[]): EventQuery[] => {
	return [...queries].sort((a, b) => {
		if (!a.data?.data || !b.data?.data) return 0;
		if (!a.data?.data) return 1;
		if (!b.data?.data) return -1;

		for (const sorter of sorters) {
			const result = sorter(a.data?.data, b.data?.data);
			if (result !== 0) return result;
		}
		return 0;
	});
};

