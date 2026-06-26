import type { ReactNode } from "react";
import { View } from "react-native";
import { Sheet } from "../../base/Sheet";
import { useComboboxCtx } from "./combobox-context";

export interface ComboboxSheetProps {
	children: ReactNode;
	search?: ReactNode;
}

export const ComboboxSheet = ({ children, search }: ComboboxSheetProps) => {
	const ctx = useComboboxCtx();

	return (
		<Sheet open={ctx.opened} onClose={ctx.close} scrollable={false}>
			{children}
		</Sheet>
	);
};
