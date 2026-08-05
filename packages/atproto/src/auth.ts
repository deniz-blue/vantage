import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";

export interface AuthSessionEntry {
	did: string;
	pds: string;
	handle?: string;
	displayName?: string;
	avatarUrl?: string;
	lastActiveAt: number;
}

export interface AuthStore {
	sessions: Record<string, AuthSessionEntry>;
	activeDid: string | null;
	addSession: (entry: AuthSessionEntry) => void;
	removeSession: (did: string) => void;
	switchAccount: (did: string) => void;
	logoutAll: () => void;
	updateSession: (did: string, patch: Partial<Omit<AuthSessionEntry, "did">>) => void;
}

export const useAuthStore = create<AuthStore>()(
	persist(
		(set) => ({
			sessions: {},
			activeDid: null,

			addSession: (entry) =>
				set((state) => ({
					sessions: { ...state.sessions, [entry.did]: entry },
					activeDid: entry.did,
				})),

			removeSession: (did) =>
				set((state) => {
					const { [did]: _, ...rest } = state.sessions;
					const remaining = Object.keys(rest);
					return {
						sessions: rest,
						activeDid: state.activeDid === did ? (remaining[0] ?? null) : state.activeDid,
					};
				}),

			switchAccount: (did) => set({ activeDid: did }),

			logoutAll: () => set({ sessions: {}, activeDid: null }),

			updateSession: (did, patch) =>
				set((state) => {
					const existing = state.sessions[did];
					if (!existing) return state;
					return {
						sessions: {
							...state.sessions,
							[did]: { ...existing, ...patch, lastActiveAt: Date.now() },
						},
					};
				}),
		}),
		{
			name: "vantage:atproto:auth",
			version: 1,
			storage: createJSONStorage(() => AsyncStorage),
		},
	),
);
