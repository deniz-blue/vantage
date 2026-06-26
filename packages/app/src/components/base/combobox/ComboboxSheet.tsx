import type { PropsWithChildren, ReactNode } from "react";
import { Sheet } from "../../base/Sheet";
import { useComboboxCtx } from "./combobox-context";

export const ComboboxSheet = ({ children }: PropsWithChildren) => {
	const ctx = useComboboxCtx();

	return (
		<Sheet open={ctx.opened} onClose={ctx.close} scrollable={false}>
			{children}
		</Sheet>
	);
};
