import { useCallback, useState } from "react";
import { produce } from "immer";
import type { Draft } from "immer";

export interface Editor<T> {
	value: T;
	update: (recipe: (draft: Draft<T>) => void) => void;
	field: <U>(selector: (value: T) => U) => Editor<U>;
}

export const useEditor = <T,>(
	initialData: T | (() => T),
) => {
	const [value, setValue] = useState<T>(initialData);

	const rootUpdate = useCallback(
		(recipe: (draft: Draft<T>) => void) => {
			setValue((prev) => produce(prev, recipe));
		},
		[],
	);

	const field = <U,>(selector: (value: T) => U): Editor<U> => ({
		value: selector(value),
		update: (recipe) => rootUpdate((d) => void recipe(selector(d as any) as any)),
		field: (sub) => field((v) => sub(selector(v))),
	});

	const editor: Editor<T> = {
		value,
		update: rootUpdate,
		field,
	};

	return { editor };
};




