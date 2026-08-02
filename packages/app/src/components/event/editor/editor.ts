import { produce, Draft } from "immer";

export interface Editor<T> {
	value: T;
	update: (recipe: (draft: Draft<NonNullable<T>>) => void) => void;
	field: <K extends keyof NonNullable<T>>(
		key: K,
		defaultValue?: NonNullable<T>[K],
	) => Editor<NonNullable<T>[K]>;
	push: NonNullable<T> extends Array<infer Item> ? (...items: Item[]) => void : never;
	removeAt: NonNullable<T> extends Array<any> ? (index: number) => void : never;
	at: NonNullable<T> extends Array<infer Item> ? (index: number) => Editor<Item> : never;
}

interface PathSegment {
	key: string | number;
	defaultValue?: any;
}

export function createEditor<T>(value: T, setValue: (updater: (prev: T) => T) => void): Editor<T> {
	return createPathEditor(value, setValue, []);
}

function createPathEditor<Root, Curr>(
	rootValue: Root,
	setRootValue: (updater: (prev: Root) => Root) => void,
	path: PathSegment[],
): Editor<Curr> {
	const currentValue = path.reduce<any>((acc, segment) => {
		if (acc === null || acc === undefined) return undefined;
		return acc[segment.key];
	}, rootValue) as Curr;

	const update = (recipe: (draft: Draft<NonNullable<Curr>>) => void) => {
		setRootValue((prevRoot) =>
			produce(prevRoot, (draft: any) => {
				let current = draft;

				for (let i = 0; i < path.length; i++) {
					const { key, defaultValue } = path[i];
					const nextSegment = path[i + 1];

					if (current[key] === undefined || current[key] === null) {
						if (defaultValue !== undefined) {
							current[key] = Array.isArray(defaultValue) ? [...defaultValue] : { ...defaultValue };
						} else if (typeof nextSegment?.key === "number") {
							current[key] = [];
						} else {
							current[key] = {};
						}
					}

					current = current[key];
				}

				recipe(current);
			}),
		);
	};

	return {
		value: currentValue,
		update,
		field: (key, defaultValue) =>
			createPathEditor(rootValue, setRootValue, [...path, { key: key as any, defaultValue }]),

		// Native array helpers
		push: ((...items: any[]) => {
			// Ensure path segment knows it's an array default
			if (path.length > 0 && path[path.length - 1].defaultValue === undefined) {
				path[path.length - 1].defaultValue = [];
			}
			update((draft: any) => {
				draft.push(...items);
			});
		}) as any,

		removeAt: ((index: number) => {
			update((draft: any) => {
				if (Array.isArray(draft)) {
					draft.splice(index, 1);
				}
			});
		}) as any,

		at: ((index: number) =>
			createPathEditor(rootValue, setRootValue, [...path, { key: index }])) as any,
	};
}
