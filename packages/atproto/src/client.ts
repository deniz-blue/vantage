import { create } from "zustand";
import { Client } from "@atcute/client";
import type { OAuthSession } from "./oauth/client";

export interface AtClientStore {
	client: Client | null;
	session: OAuthSession | null;
}

export const useAtClient = create<AtClientStore>(() => ({
	client: null,
	session: null,
}));
