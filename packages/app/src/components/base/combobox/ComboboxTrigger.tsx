import { IconChevronRight } from "@tabler/icons-react-native";
import type { ReactNode } from "react";
import { Box } from "../base/Box";
import { Button } from "../base/Button";
import { Colors } from "../../theme/colors";
import { Sizing } from "../../theme/sizing";
import { useComboboxCtx } from "./combobox-context";

export interface ComboboxTriggerProps {
	children: ReactNode;
}

export const ComboboxTrigger = ({ children }: ComboboxTriggerProps) => {
	const ctx = useComboboxCtx();
	return (
		<Button
			variant="default"
			rightSection={<IconChevronRight size={18} color={Colors.TextDimmed} />}
			onPress={ctx.open}
			radius={Sizing.radiusLg}
		>
			<Box flex={1} direction="row" align="center" gap={10}>{children}</Box>
		</Button>
	);
};
