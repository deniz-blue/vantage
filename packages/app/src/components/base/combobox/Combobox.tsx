import { useCallback, useMemo, useRef, useState, type ReactNode } from "react";
import { Ctx, type ComboboxContext } from "./combobox-context";
import { SheetRef } from "../sheet/Sheet";

export interface ComboboxProps<T> {
	children: ReactNode;
	value: T;
	onChange: (value: T) => void;
}

export const Combobox = <T,>({ children, value, onChange }: ComboboxProps<T>) => {
	const sheet = useRef<SheetRef>(null);
	const [search, setSearch] = useState("");

	const open = useCallback(() => sheet.current?.present(), []);
	const close = useCallback(() => {
		sheet.current?.dismiss();
		setSearch("");
	}, []);

	const ctx = useMemo<ComboboxContext<T>>(
		() => ({ value, onChange, search, setSearch, sheet, open, close }) as ComboboxContext<T>,
		[value, onChange, search, sheet, close],
	);

	return <Ctx.Provider value={ctx as ComboboxContext<unknown>}>{children}</Ctx.Provider>;
};
