import { IconChevronRight } from "@tabler/icons-react-native";
import { Button, ButtonProps } from "../../base/Button";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { IconSize } from "../../../theme/sizing";

export const ComboboxTrigger = (props: ButtonProps) => {
	const ctx = useComboboxCtx();
	return (
		<Button
			variant="default"
			rightSection={<IconChevronRight size={IconSize.xs} color={Colors.TextDimmed} />}
			onPress={ctx.open}
			{...props}
		/>
	);
};
