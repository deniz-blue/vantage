import type { ReactNode } from "react";
import { Sheet } from "../base/Sheet";
import { useComboboxCtx } from "./combobox-context";

export interface ComboboxSheetProps {
	children: ReactNode;
	height?: number;
}

export const ComboboxSheet = ({ children, height = 0.7 }: ComboboxSheetProps) => {
	const ctx = useComboboxCtx();
	return (
		<Sheet open={ctx.opened} onClose={ctx.close} height={height}>
			{children}
		</Sheet>
	);
};
