import { ComponentType, Fragment, ReactNode, useMemo } from "react";
import { Editor } from "./editor";
import { Box } from "../../base/Box";
import { Line } from "../../base/Divider";
import { Button } from "../../base/button/Button";
import { Colors } from "../../../theme/colors";
import { FontSize, IconSize } from "../../../theme/sizing";
import { IconPlus } from "@tabler/icons-react-native";
import { Text } from "../../base/Text";

export const FormSectionList = <T,>({
	title,
	editor,
	onAdd,
	emptyText,
	renderItem,
	onDelete,
}: {
	title: string;
	editor: Editor<T[] | undefined>;
	onAdd: () => void;
	emptyText: ReactNode;
	renderItem: ComponentType<{ onDelete: () => void; editor: Editor<T>; index: number }>;
	onDelete?: (props: { index: number; value: T }) => void;
}) => {
	return (
		<Box gap="md">
			<Box direction="row" gap="sm" align="center">
				<Text c="TextDimmed" fw="bold">
					{title}
				</Text>
				<Line />
				<Button
					size="sm"
					onPress={onAdd}
					rightSection={<IconPlus size={IconSize.xs} color={Colors.Text} />}
				>
					Add
				</Button>
			</Box>

			{!editor.value?.length && (
				<Box align="center">
					<Text c="TextDimmed" fz={FontSize.sm}>
						{emptyText}
					</Text>
				</Box>
			)}

			<EditorList editor={editor} renderItem={renderItem} onDelete={onDelete} />
		</Box>
	);
};

export const EditorList = <T,>({
	editor,
	renderItem: ItemComponent,
	onDelete,
}: {
	editor: Editor<T[] | undefined>;
	renderItem: ComponentType<{ onDelete: () => void; editor: Editor<T>; index: number }>;
	onDelete?: (props: { index: number; value: T }) => void;
}) => {
	const children = useMemo(() => {
		if (!editor.value?.length) return null;
		return editor.value.map((_, i) => (
			<Fragment key={i}>
				<ItemComponent
					editor={editor.at(i)}
					index={i}
					onDelete={() => {
						editor.update((d) => void d.splice(i, 1));
						onDelete?.({ index: i, value: editor.value![i]! });
					}}
				/>
			</Fragment>
		));
	}, [editor, ItemComponent, onDelete]);

	return children;
};
