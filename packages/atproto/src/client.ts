import { create } from "zustand";
import { Client } from "@atcute/client";
import type { OAuthSession } from "./oauth/client";

export interface AtClientStore {
	client: Client | null;
	session: OAuthSession | null;
	ready: boolean;
	setClient: (client: Client, session: OAuthSession) => void;
	clear: () => void;
}

export const useAtClient = create<AtClientStore>((set) => ({
	client: null,
	session: null,
	ready: false,
	setClient: (client, session) => set({ client, session, ready: true }),
	clear: () => set({ client: null, session: null, ready: false }),
}));
