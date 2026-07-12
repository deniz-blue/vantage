import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";

export interface LocaleStore {
	language: string;
	setLanguage: (lang: string) => void;
	timezone: string;
	setTimezone: (tz: string) => void;
}

const detectedTimezone = (): string => Intl.DateTimeFormat().resolvedOptions().timeZone ?? "UTC";

export const useLocaleStore = create<LocaleStore>()(
	persist(
		(set) => ({
			language: "en",
			setLanguage: (lang: string) => set({ language: lang }),
			timezone: detectedTimezone(),
			setTimezone: (tz: string) => set({ timezone: tz }),
		}),
		{
			name: "vantage-locale",
			version: 1,
			storage: createJSONStorage(() => AsyncStorage),
		},
	),
);
