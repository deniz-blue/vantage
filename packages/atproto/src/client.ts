import { create } from "zustand";
import { Client } from "@atcute/client";
import { AtOAuthClient, type OAuthSession } from "./oauth/client";
import type { AtprotoDid } from "@atcute/lexicons/syntax";
import { useAtAccounts } from "./accounts";

export interface AtClientStore {
	client: Client | null;
	session: OAuthSession | null;
	signIn: (did: AtprotoDid) => Promise<void>;
}

export const useAtClient = create<AtClientStore>((set) => ({
	client: null,
	session: null,
	signIn: async (did: AtprotoDid) => {
		console.log("Signing in with DID:", did);
		const session = await AtOAuthClient.restore(did);
		const client = new Client({ handler: session });
		set({ client, session });
		useAtAccounts.getState().markActive(did);
		console.log("Signed in with DID:", did);
	},
}));
