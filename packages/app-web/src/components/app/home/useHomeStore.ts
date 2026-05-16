import { create } from "zustand";
import { persist } from "zustand/middleware";
import { immer } from "zustand/middleware/immer";

export interface HomeWidgetMap {
	"vantage.events.upcoming": {};
	"wttr.in": {};
};

export type THomeWidget = {
	[Ty in keyof HomeWidgetMap]: HomeWidgetMap[Ty] & { $type: Ty };
}[keyof HomeWidgetMap];

export interface HomeState {
	widgets: THomeWidget[];
};

export const useHomeStore = create<HomeState>()(
	persist(
		immer((set, get) => ({
			widgets: [
				{ $type: "vantage.events.upcoming" },
				{ $type: "wttr.in" },
			],
		})),
		{
			name: "vantage:home",
			version: 1,
		},
	),
);
