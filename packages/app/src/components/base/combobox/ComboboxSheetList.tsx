import { useCallback, useRef, type ReactNode } from "react";
import { LayoutChangeEvent, ScrollView, TouchableOpacity } from "react-native";
import { IconCheck } from "@tabler/icons-react-native";
import { Box } from "../Box";
import { Colors } from "../../../theme/colors";
import { useComboboxCtx } from "./combobox-context";
import { SheetScrollView } from "../Sheet";

export interface ComboboxListProps<T> {
	data: readonly T[];
	renderItem: (item: T, selected: boolean) => ReactNode;
	filter?: (item: T, search: string) => boolean;
}

export const ComboboxSheetList = <T,>({ data, renderItem, filter }: ComboboxListProps<T>) => {
	const ctx = useComboboxCtx<T>();
	const scrollRef = useRef<ScrollView>(null);

	const items = data.filter((item) =>
		ctx.search.trim() && filter ? filter(item, ctx.search) : true,
	);

	const onPress = useCallback(
		(item: T) => {
			ctx.onChange(item);
			ctx.close();
		},
		[ctx],
	);

	const onSelectedLayout = useCallback((e: LayoutChangeEvent) => {
		scrollRef.current?.scrollTo({ y: e.nativeEvent.layout.y, animated: false });
	}, []);

	return (
		<Box flex={1}>
			<SheetScrollView ref={scrollRef}>
				<Box py={4}>
					{items.map((item, i) => {
						const selected = item === ctx.value;
						return (
							<Box
								key={i}
								component={TouchableOpacity}
								px={16}
								py={14}
								mx={8}
								my={2}
								radius={10}
								activeOpacity={0.7}
								onPress={() => onPress(item)}
								onLayout={selected ? onSelectedLayout : undefined}
								direction="row"
								align="center"
								bg={selected ? Colors.PrimaryLight + "33" : undefined}
							>
								{renderItem(item, selected)}
								{selected && <IconCheck size={20} color={Colors.Primary} />}
							</Box>
						);
					})}
				</Box>
			</SheetScrollView>
		</Box>
	);
};
