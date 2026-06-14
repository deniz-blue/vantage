import { TextInput } from "react-native";
import { IconSearch } from "@tabler/icons-react-native";
import { Box } from "../base/Box";
import { Colors } from "../../theme/colors";
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
			<Box direction="row" align="center" bg={Colors.BackgroundLight} radius={10} px={12} gap={8}>
				<IconSearch size={18} color={Colors.TextDimmed} />
				<Box flex={1}>
					<TextInput
						value={ctx.search}
						onChangeText={ctx.setSearch}
						placeholder={placeholder}
						placeholderTextColor={Colors.TextDimmed}
						style={{
							color: Colors.Text,
							fontSize: 15,
							paddingVertical: 10,
						}}
					/>
				</Box>
			</Box>
		</Box>
	);
};
