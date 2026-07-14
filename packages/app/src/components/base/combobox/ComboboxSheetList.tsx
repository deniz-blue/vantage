import { ComponentType, useCallback, useMemo } from "react";
import { IconCheck } from "@tabler/icons-react-native";
import { Box } from "../Box";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { FlashList } from "@shopify/flash-list";
import { KeyboardAwareScrollView } from "react-native-keyboard-controller";
import { ComboboxSearch } from "./ComboboxSearch";
import { FlatList } from "react-native";
import { Button } from "../button/Button";

export interface ComboboxListProps<T> {
	data: readonly T[];
	searchable?: boolean;
	renderItem: ComponentType<{ value: T; selected: boolean }>;
	filter?: (item: T, search: string) => boolean;
	keyExtractor?: (item: T) => string;
}

export const ComboboxSheetList = <T,>({
	data,
	renderItem: ItemComponent,
	filter,
	searchable,
	keyExtractor,
}: ComboboxListProps<T>) => {
	const ctx = useComboboxCtx<T>();

	const onPress = useCallback(
		(item: T) => {
			ctx.onChange(item);
			ctx.close();
		},
		[ctx],
	);

	type ListItem = { kind: "item"; value: T } | { kind: "search" };

	const flashListRenderItem = useCallback(
		({ item }: { item: ListItem }) => {
			if (item.kind === "search") return <ComboboxSearch />;

			const selected = item === ctx.value;

			return (
				<Button m="xs" onPress={() => onPress(item.value)}>
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

	const items: ListItem[] = searchable ? [{ kind: "search" }, ...filteredItems] : filteredItems;

	return (
		<Box flex={1}>
			<FlatList
				data={items}
				renderItem={flashListRenderItem}
				renderScrollComponent={(props) => <KeyboardAwareScrollView {...props} />}
				stickyHeaderIndices={searchable ? [0] : undefined}
				keyExtractor={(item, index) => {
					if (item.kind === "search") return "search";
					if (keyExtractor) return keyExtractor(item.value);
					if (typeof item.value === "string") return item.value;
					return String(index);
				}}
				// initialScrollIndex={initialScrollIndex}
				style={{ flex: 1 }}
			/>
		</Box>
	);
};
