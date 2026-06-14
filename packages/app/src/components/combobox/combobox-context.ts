import { createContext, useContext } from "react";

export interface ComboboxContext<T> {
	value: T;
	onChange: (value: T) => void;
	search: string;
	setSearch: (value: string) => void;
	opened: boolean;
	open: () => void;
	close: () => void;
}

const Ctx = createContext<ComboboxContext<unknown> | null>(null);

export { Ctx };

export function useComboboxCtx<T>(): ComboboxContext<T> {
	const ctx = useContext(Ctx);
	if (!ctx) throw new Error("useComboboxCtx must be inside <Combobox>");
	return ctx as ComboboxContext<T>;
}
