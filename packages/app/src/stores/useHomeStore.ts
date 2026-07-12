import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import { immer } from "zustand/middleware/immer";
import AsyncStorage from "@react-native-async-storage/async-storage";

export interface HomeWidgetMap {
	"vantage.events.upcoming": {};
	"wttr.in": {};
}

export type THomeWidget = {
	[Ty in keyof HomeWidgetMap]: HomeWidgetMap[Ty] & { $type: Ty };
}[keyof HomeWidgetMap];

export interface HomeState {
	widgets: THomeWidget[];
}

export const useHomeStore = create<HomeState>()(
	persist(
		immer(() => ({
			widgets: [{ $type: "wttr.in" }, { $type: "vantage.events.upcoming" }],
		})),
		{
			name: "vantage:home",
			version: 1,
			storage: createJSONStorage(() => AsyncStorage),
		},
	),
);
