import { produce } from "immer";
import type { Draft } from "immer";

export interface Editor<T> {
	value: T;
	update: (recipe: (draft: Draft<T>) => void) => void;
	field: <U>(selector: (value: T) => U) => Editor<U>;
}

export const createEditor = <T>(value: T, setValue: (update: (prev: T) => T) => void) => {
	const rootUpdate = (recipe: (draft: Draft<T>) => void) => {
		setValue((prev) => produce(prev, recipe));
	};

	const field = <U>(selector: (value: T) => U): Editor<U> => ({
		value: selector(value),
		update: (recipe) => rootUpdate((d) => void recipe(selector(d as any) as any)),
		field: (sub) => field((v) => sub(selector(v))),
	});

	const editor: Editor<T> = {
		value,
		update: rootUpdate,
		field,
	};

	return editor;
};
