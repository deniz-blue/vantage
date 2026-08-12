import { createContext, RefObject, useContext } from "react";
import { SheetRef } from "../sheet/Sheet";

export interface ComboboxContext<T> {
	onOptionSubmit: (value: T) => void;

	search: string;
	setSearch: (value: string) => void;

	sheet: RefObject<SheetRef | null>;
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
