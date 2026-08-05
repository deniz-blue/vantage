import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";

export interface AtAccount {
	did: string;
	pds: string;
	handle?: string;
	displayName?: string;
	avatarUrl?: string;
	lastActiveAt: number;
}

export interface AtAccountsStore {
	accounts: Record<string, AtAccount>;
	activeDid: string | null;
	addAccount: (entry: AtAccount) => void;
	removeAccount: (did: string) => void;
	switchAccount: (did: string) => void;
	logoutAll: () => void;
	updateAccount: (did: string, patch: Partial<Omit<AtAccount, "did">>) => void;
}

export const useAtAccounts = create<AtAccountsStore>()(
	persist(
		(set) => ({
			accounts: {},
			activeDid: null,

			addAccount: (entry) =>
				set((state) => ({
					accounts: { ...state.accounts, [entry.did]: entry },
					activeDid: entry.did,
				})),

			removeAccount: (did) =>
				set((state) => {
					const { [did]: _, ...rest } = state.accounts;
					const remaining = Object.keys(rest);
					return {
						accounts: rest,
						activeDid: state.activeDid === did ? (remaining[0] ?? null) : state.activeDid,
					};
				}),

			switchAccount: (did) => set({ activeDid: did }),

			logoutAll: () => set({ accounts: {}, activeDid: null }),

			updateAccount: (did, patch) =>
				set((state) => {
					const existing = state.accounts[did];
					if (!existing) return state;
					return {
						accounts: {
							...state.accounts,
							[did]: { ...existing, ...patch, lastActiveAt: Date.now() },
						},
					};
				}),
		}),
		{
			name: "vantage:atproto:accounts",
			version: 1,
			storage: createJSONStorage(() => AsyncStorage),
		},
	),
);
