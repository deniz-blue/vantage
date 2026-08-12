import { ComponentType, useCallback, useMemo } from "react";
import { IconCheck } from "@tabler/icons-react-native";
import { Box } from "../Box";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { FlatList } from "react-native";
import { Button } from "../button/Button";
import { Spacing } from "../../../theme/spacing";

export interface ComboboxListProps<T> {
	data: readonly T[];
	renderItem: ComponentType<{ value: T; selected: boolean }>;
	isSelected?: (item: T) => boolean;
	filter?: (item: T, search: string) => boolean;
	keyExtractor?: (item: T) => string;
	closeOnSelect?: boolean;
	withPadding?: boolean;
}

export const ComboboxList = <T,>({
	data,
	renderItem: ItemComponent,
	isSelected = () => false,
	filter,
	keyExtractor,
	closeOnSelect,
	withPadding,
}: ComboboxListProps<T>) => {
	const ctx = useComboboxCtx<T>();

	const onPress = useCallback(
		(item: T) => {
			ctx.onOptionSubmit(item);
			if (closeOnSelect) ctx.close();
		},
		[ctx, closeOnSelect],
	);

	const flashListRenderItem = useCallback(
		({ item }: { item: T }) => {
			const selected = isSelected(item);

			return (
				<Button m="xs" selected={selected} onPress={() => onPress(item)}>
					<Box direction="row" flex={1}>
						<ItemComponent value={item} selected={selected} />
						{selected && <IconCheck size={20} color={Colors.Primary} />}
					</Box>
				</Button>
			);
		},
		[ItemComponent, isSelected, onPress],
	);

	const items = useMemo(
		() => data.filter((item) => (ctx.search.trim() && filter ? filter(item, ctx.search) : true)),
		[data, ctx.search, filter],
	);

	return (
		<FlatList
			data={items}
			renderItem={flashListRenderItem}
			keyExtractor={(item, index) => {
				if (keyExtractor) return keyExtractor(item);
				if (typeof item === "string") return item;
				return String(index);
			}}
			contentContainerStyle={{ padding: withPadding ? Spacing.md : undefined }}
		/>
	);
};
