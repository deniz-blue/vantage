import { useCallback, useEffect, useMemo, useRef, type ReactNode } from "react";
import { FlatList, TouchableOpacity } from "react-native";
import type { FlatList as FlatListType } from "react-native";
import { IconCheck } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { useComboboxCtx } from "./combobox-context";

export interface ComboboxListProps<T> {
	data: readonly T[];
	renderItem: (item: T, selected: boolean) => ReactNode;
	filter?: (item: T, search: string) => boolean;
}

export const ComboboxList = <T,>({
	data,
	renderItem,
	filter,
}: ComboboxListProps<T>) => {
	const ctx = useComboboxCtx<T>();
	const ref = useRef<FlatListType<T>>(null);

	const items = useMemo(() => {
		if (!ctx.search.trim() || !filter) return data;
		return data.filter((item) => filter(item, ctx.search));
	}, [data, ctx.search, filter]);

	useEffect(() => {
		const idx = items.indexOf(ctx.value);
		if (idx < 0) return;
		const timer = setTimeout(() => {
			ref.current?.scrollToIndex({ index: idx, animated: true, viewPosition: 0.3 });
		}, 350);
		return () => clearTimeout(timer);
	}, [items, ctx.value]);

	const onScrollToIndexFailed = useCallback(
		(info: { index: number; averageItemLength: number }) => {
			ref.current?.scrollToOffset({
				offset: info.index * info.averageItemLength,
				animated: true,
			});
		},
		[],
	);

	return (
		<FlatList
			ref={ref}
			data={items}
			keyExtractor={(_item, i) => String(i)}
			style={{ flex: 1 }}
			keyboardShouldPersistTaps="handled"
			onScrollToIndexFailed={onScrollToIndexFailed}
			contentContainerStyle={{ paddingVertical: 4 }}
			renderItem={({ item }) => {
				const selected = item === ctx.value;
				return (
					<TouchableOpacity
						onPress={() => { ctx.onChange(item); ctx.close(); }}
						activeOpacity={0.7}
						style={{
							paddingHorizontal: 16,
							paddingVertical: 14,
							backgroundColor: selected ? Colors.PrimaryLight + "33" : "transparent",
							flexDirection: "row",
							alignItems: "center",
							borderRadius: 10,
							marginHorizontal: 8,
							marginVertical: 2,
						}}
					>
						{renderItem(item, selected)}
						{selected && <IconCheck size={20} color={Colors.Primary} />}
					</TouchableOpacity>
				);
			}}
		/>
	);
};
