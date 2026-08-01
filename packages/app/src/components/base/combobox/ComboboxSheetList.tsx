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
	filter?: (item: T, search: string) => boolean;
	keyExtractor?: (item: T) => string;
	withPadding?: boolean;
}

export const ComboboxSheetList = <T,>({
	data,
	renderItem: ItemComponent,
	filter,
	keyExtractor,
	withPadding,
}: ComboboxListProps<T>) => {
	const ctx = useComboboxCtx<T>();

	const onPress = useCallback(
		(item: T) => {
			ctx.onChange(item);
			ctx.close();
		},
		[ctx],
	);

	type ListItem = { kind: "item"; value: T };

	const flashListRenderItem = useCallback(
		({ item }: { item: ListItem }) => {
			const selected = item === ctx.value;

			return (
				<Button m="xs" selected={selected} onPress={() => onPress(item.value)}>
					<Box direction="row" flex={1}>
						<ItemComponent value={item.value} selected={selected} />
						{selected && <IconCheck size={20} color={Colors.Primary} />}
					</Box>
				</Button>
			);
		},
		[ItemComponent, ctx.value, onPress],
	);

	// const initialScrollIndex = useMemo(() => {
	// 	const index = data.findIndex((item) => item === ctx.value);
	// 	if (index === -1) return undefined;
	// 	return searchable ? index + 1 : index;
	// }, [data, ctx.value, searchable]);

	const filteredData = useMemo(
		() => data.filter((item) => (ctx.search.trim() && filter ? filter(item, ctx.search) : true)),
		[data, ctx.search, filter],
	);

	const filteredItems: ListItem[] = useMemo(
		() =>
			filteredData.map((item) => ({
				kind: "item",
				value: item,
			})),
		[filteredData],
	);

	const items: ListItem[] = filteredItems;

	return (
		<FlatList
			data={items}
			renderItem={flashListRenderItem}
			keyExtractor={(item, index) => {
				if (keyExtractor) return keyExtractor(item.value);
				if (typeof item.value === "string") return item.value;
				return String(index);
			}}
			contentContainerStyle={{ padding: withPadding ? Spacing.md : undefined }}
		/>
	);
};
