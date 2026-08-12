import { useCallback, useMemo, useRef, useState, type ReactNode } from "react";
import { Ctx, type ComboboxContext } from "./combobox-context";
import { SheetRef } from "../sheet/Sheet";

export interface ComboboxProps<T> {
	children: ReactNode;
	onOptionSubmit: (value: T) => void;
}

export const Combobox = <T,>({ children, onOptionSubmit }: ComboboxProps<T>) => {
	const sheet = useRef<SheetRef>(null);
	const [search, setSearch] = useState("");

	const open = useCallback(() => sheet.current?.present(), []);
	const close = useCallback(() => {
		sheet.current?.dismiss();
		setSearch("");
	}, []);

	const ctx = useMemo<ComboboxContext<T>>(
		() => ({ onOptionSubmit, search, setSearch, sheet, open, close }) as ComboboxContext<T>,
		[onOptionSubmit, search, setSearch, sheet, open, close],
	);

	return <Ctx value={ctx as ComboboxContext<unknown>}>{children}</Ctx>;
};
