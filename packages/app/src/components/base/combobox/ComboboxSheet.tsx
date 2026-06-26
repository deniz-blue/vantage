import type { ReactNode } from "react";
import { View } from "react-native";
import { Sheet } from "../../base/Sheet";
import { useComboboxCtx } from "./combobox-context";

const SEARCH_HEIGHT = 64;

export interface ComboboxSheetProps {
	children: ReactNode;
	search?: ReactNode;
	height?: number;
}

export const ComboboxSheet = ({ children, search, height = 0.7 }: ComboboxSheetProps) => {
	const ctx = useComboboxCtx();

	return (
		<Sheet open={ctx.opened} onClose={ctx.close} height={height} scrollable={false}>
			{search && (
				<View
					style={{
						position: "absolute",
						top: 0,
						left: 0,
						right: 0,
						zIndex: 1,
					}}
				>
					{search}
				</View>
			)}

			<View style={{ flex: 1, paddingTop: search ? SEARCH_HEIGHT : 0 }}>
				{children}
			</View>
		</Sheet>
	);
};
