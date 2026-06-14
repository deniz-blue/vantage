import { useCallback, useMemo, useState, type ReactNode } from "react";
import { Ctx, type ComboboxContext } from "./combobox-context";

export interface ComboboxProps<T> {
	children: ReactNode;
	value: T;
	onChange: (value: T) => void;
}

export const Combobox = <T,>({ children, value, onChange }: ComboboxProps<T>) => {
	const [opened, setOpened] = useState(false);
	const [search, setSearch] = useState("");

	const open = useCallback(() => setOpened(true), []);
	const close = useCallback(() => {
		setOpened(false);
		setSearch("");
	}, []);

	const ctx = useMemo<ComboboxContext<T>>(
		() => ({ value, onChange, search, setSearch, opened, open, close }) as ComboboxContext<T>,
		[value, onChange, search, opened, close],
	);

	return <Ctx.Provider value={ctx as ComboboxContext<unknown>}>{children}</Ctx.Provider>;
};
