export namespace Feed {
	export interface TypeMap {
		jsonfeed: {
			url: string;
		};
		at: {
			repo: string;
		};
	}

	export type Info<T extends keyof TypeMap = keyof TypeMap> = {
		type: T;
	} & TypeMap[T];
}
