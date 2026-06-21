import { IconChevronRight } from "@tabler/icons-react-native";
import type { ReactNode } from "react";
import { Box } from "../../base/Box";
import { Button } from "../../base/Button";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { IconSize } from "../../../theme/sizing";

export interface ComboboxTriggerProps {
	children: ReactNode;
}

export const ComboboxTrigger = ({ children }: ComboboxTriggerProps) => {
	const ctx = useComboboxCtx();
	return (
		<Button
			variant="default"
			rightSection={<IconChevronRight size={IconSize.xs} color={Colors.TextDimmed} />}
			onPress={ctx.open}
		>
			<Box flex={1} direction="row" align="center" gap="xs">{children}</Box>
		</Button>
	);
};
