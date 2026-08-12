import { OpenEvnt } from "@evnt/types";
import { Editor } from "./editor";
import { ComponentType, useCallback, useMemo } from "react";
import ReorderableList, { ReorderableListReorderEvent } from "react-native-reorderable-list";

export const FormComponents = <T extends { $type: string }>({
	type,
	editor,
	renderItem: ItemComponent,
}: {
	type: T["$type"];
	editor: Editor<OpenEvnt>;
	renderItem: ComponentType<{ editor: Editor<T>; onDelete: () => void }>;
}) => {
	type ListItem = {
		index: number;
		component: T;
	};

	const components = useMemo(() => editor.field("components", []), [editor]);

	const items = useMemo(
		() =>
			components.value
				?.map((c, index) => ({ index, component: c }))
				.filter((item): item is ListItem => item.component.$type === type) ?? [],
		[components.value],
	);

	const renderItem = useCallback(
		({ item }: { item: ListItem }) => (
			<ItemComponent
				onDelete={() => components.update((d) => void d.splice(item.index, 1))}
				editor={components.at(item.index) as Editor<T>}
			/>
		),
		[editor],
	);

	const onReorder = useCallback(
		({ from, to }: ReorderableListReorderEvent) => {
			editor.update((d) => {
				if (!d.components) return;
				const filtered = d.components.filter((c): c is T => c.$type === type);
				const link = filtered[from];
				filtered.splice(from, 1);
				filtered.splice(to, 0, link);
				d.components = [...d.components.filter((c) => c.$type !== type), ...filtered];
			});
		},
		[editor],
	);

	return (
		<ReorderableList
			data={items}
			renderItem={renderItem}
			onReorder={onReorder}
			keyExtractor={({ index }) => index.toString()}
			scrollEnabled={false}
		/>
	);
};
