import type { PropsWithChildren } from "react";
import { Sheet, SheetProps } from "../sheet/Sheet";
import { useComboboxCtx } from "./combobox-context";

export const ComboboxSheet = (props: PropsWithChildren<Omit<SheetProps, "ref">>) => {
	const ctx = useComboboxCtx();

	return <Sheet {...props} ref={ctx.sheet} scrollable={props.scrollable ?? true} />;
};
