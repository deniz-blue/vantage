import { AtprotoDid, Handle } from "@atcute/lexicons/syntax";

export namespace Feed {
	export interface TypeMap {
		jsonfeed: {
			url: string;
		};
		at: {
			repo: AtprotoDid | Handle;
			collection: "directory.evnt.event" | "community.lexicon.calendar.event";
		};
	}

	export type InfoUnion = {
		[K in keyof TypeMap]: { type: K } & TypeMap[K];
	}[keyof TypeMap];

	export type Info<T extends keyof TypeMap = keyof TypeMap> = Extract<InfoUnion, { type: T }>;
}
