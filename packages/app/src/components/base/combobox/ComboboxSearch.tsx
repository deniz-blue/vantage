import { IconSearch } from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { TextInput } from "../../base/TextInput";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";

export interface ComboboxSearchProps {
	placeholder?: string;
}

export const ComboboxSearch = ({ placeholder = "Search…" }: ComboboxSearchProps) => {
	const ctx = useComboboxCtx();
	return (
		<Box
			p="md"
			style={{ borderBottomWidth: 1, borderBottomColor: Colors.BackgroundLight }}
		>
			<TextInput
				value={ctx.search}
				onChangeText={ctx.setSearch}
				placeholder={placeholder}
				leftSection={<IconSearch size={20} color={Colors.TextDimmed} />}
			/>
		</Box>
	);
};
