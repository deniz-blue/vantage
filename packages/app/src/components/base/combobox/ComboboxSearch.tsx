import { IconSearch } from "@tabler/icons-react-native";
import { TextInput } from "../input/TextInput";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { Card } from "../Card";
import { memo } from "react";

export interface ComboboxSearchProps {
	placeholder?: string;
}

export const ComboboxSearch = memo(({ placeholder = "Search…" }: ComboboxSearchProps) => {
	const ctx = useComboboxCtx();
	return (
		<Card m="xs" bg={Colors.Background}>
			<TextInput
				value={ctx.search}
				onChangeText={ctx.setSearch}
				placeholder={placeholder}
				leftSection={<IconSearch size={20} color={Colors.TextDimmed} />}
			/>
		</Card>
	);
});
