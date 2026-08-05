import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";
import type { AtprotoDid } from "@atcute/lexicons/syntax";
import { immer } from "zustand/middleware/immer";

export interface AtAccount {
	did: AtprotoDid;
	pds: string;
	lastActiveAt: number;
}

export interface AtAccountsStore {
	accounts: Record<string, AtAccount>;
	activeDid: AtprotoDid | null;
	addAccount: (entry: AtAccount) => void;
	removeAccount: (did: AtprotoDid) => void;
	markActive: (did: AtprotoDid) => void;
}

export const useAtAccounts = create<AtAccountsStore>()(
	persist(
		immer((set) => ({
			accounts: {},
			activeDid: null,

			addAccount: (entry) =>
				set((state) => {
					state.accounts[entry.did] = { ...entry, lastActiveAt: Date.now() };
				}),

			removeAccount: (did) =>
				set((state) => {
					delete state.accounts[did];
				}),

			markActive: (did) =>
				set((state) => {
					if (state.accounts[did]) state.accounts[did].lastActiveAt = Date.now();
					state.activeDid = did;
				}),
		})),
		{
			name: "vantage:atproto:accounts",
			version: 1,
			storage: createJSONStorage(() => AsyncStorage),
		},
	),
);
