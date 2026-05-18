import { create } from "zustand";
import { immer } from "zustand/middleware/immer";

export type CalendarView = "month" | "month-mobile" | "timeline";

export interface CalendarStore {
	view: CalendarView;
	date: `${number}-${number}-${number}`;
	viewDelta: (n: 1 | -1) => void;
};

export const useCalendarStore = create<CalendarStore>()(
	immer((set) => ({
		view: "month",
		date: Temporal.Now.plainDateISO().toString() as `${number}-${number}-${number}`,
		viewDelta: (n) => set((state) => {
			let [year, month, day] = state.date.split("-").map(Number) as [number, number, number];
			month += n;
			if (month < 1) {
				month = 12;
				year -= 1;
			} else if (month > 12) {
				month = 1;
				year += 1;
			}
			state.date = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}` as any;
		}),
	}))
);
