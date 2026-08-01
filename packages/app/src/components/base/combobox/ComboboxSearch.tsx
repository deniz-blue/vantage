import { IconSearch } from "@tabler/icons-react-native";
import { TextInput } from "../input/TextInput";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { memo } from "react";
import { Card } from "../Card";
import { Radius } from "../../../theme/sizing";

export interface ComboboxSearchProps {
	placeholder?: string;
}

export const ComboboxSheetSearch = memo(({ placeholder = "Search…" }: ComboboxSearchProps) => {
	return (
		<Card p={0} radius={Radius.Default} mx="sm" mb="sm">
			<ComboboxSearch placeholder={placeholder} />
		</Card>
	);
});

export const ComboboxSearch = memo(({ placeholder = "Search…" }: ComboboxSearchProps) => {
	const ctx = useComboboxCtx();
	return (
		<TextInput
			value={ctx.search}
			onChangeText={ctx.setSearch}
			placeholder={placeholder}
			leftSection={<IconSearch size={20} color={Colors.TextDimmed} />}
		/>
	);
});
