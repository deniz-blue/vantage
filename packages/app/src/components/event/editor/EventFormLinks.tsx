import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { InputWrapper } from "../../base/input/InputWrapper";
import ReorderableList, {
	ReorderableListReorderEvent,
	useReorderableDrag,
} from "react-native-reorderable-list";
import { Sheet, SheetRef } from "../../base/sheet/Sheet";
import { useCallback, useMemo, useRef, useState } from "react";
import { createEditor, Editor } from "./editor";
import { LinkComponent, OpenEvnt } from "@evnt/types";
import { LinkEditSheetContent } from "./LinkEditSheetContent";
import { IconCheck, IconGripHorizontal, IconLink, IconTrash } from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { IconSize } from "../../../theme/sizing";
import { ButtonBase } from "../../base/ButtonBase";
import { TransText } from "../../core/TransText";

const createDraftLink = (): LinkComponent => ({
	$type: "directory.evnt.component.link",
	url: "",
});

export const EventFormLinks = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	const addLinkSheet = useRef<SheetRef>(null);
	const [draftLink, setDraftLink] = useState<LinkComponent>(createDraftLink());

	type ListItem = {
		index: number;
		link: LinkComponent;
	};

	const components = useMemo(() => editor.field("components", []), [editor]);

	const onAddDraftLink = () => {
		components.push(draftLink);
		addLinkSheet.current?.dismiss();
		setDraftLink(createDraftLink());
	};

	const items = useMemo(
		() =>
			components.value
				?.map((c, index) => ({ index, link: c }))
				.filter((item): item is ListItem => item.link.$type === "directory.evnt.component.link") ??
			[],
		[components.value],
	);

	const renderItem = useCallback(
		({ item }: { item: ListItem }) => (
			<EventFormLinksItem
				onDelete={() => components.update((d) => void d.splice(item.index, 1))}
				editor={components.at(item.index) as Editor<LinkComponent>}
			/>
		),
		[editor],
	);

	const onReorder = useCallback(
		({ from, to }: ReorderableListReorderEvent) => {
			editor.update((d) => {
				if (!d.components) return;
				const links = d.components.filter(
					(c): c is LinkComponent => c.$type === "directory.evnt.component.link",
				);
				const link = links[from];
				links.splice(from, 1);
				links.splice(to, 0, link);
				d.components = [
					...d.components.filter((c) => c.$type !== "directory.evnt.component.link"),
					...links,
				];
			});
		},
		[editor],
	);

	return (
		<Box gap="xs">
			<Box direction="row" gap="sm" justify="space-between" align="center">
				<InputWrapper label="Links" />
				<InputWrapper description="Long press to reorder" />
			</Box>

			<ReorderableList
				data={items}
				renderItem={renderItem}
				onReorder={onReorder}
				keyExtractor={({ index }) => index.toString()}
				scrollEnabled={false}
			/>

			<Sheet ref={addLinkSheet}>
				<Box gap="md">
					<LinkEditSheetContent editor={createEditor(draftLink, setDraftLink)} />

					<Button onPress={onAddDraftLink} disabled={!draftLink.url.trim()}>
						Add Link
					</Button>
				</Box>
			</Sheet>

			<Button onPress={() => addLinkSheet.current?.present()}>Add Link</Button>
		</Box>
	);
};

export const EventFormLinksItem = ({
	editor,
	onDelete,
}: {
	editor: Editor<LinkComponent>;
	onDelete: () => void;
}) => {
	const sheet = useRef<SheetRef>(null);
	const startDrag = useReorderableDrag();

	return (
		<Box direction="row" gap="sm" align="center" my="xs">
			<ButtonBase onPressIn={() => startDrag()}>
				<IconGripHorizontal size={IconSize.sm} color={Colors.Text} />
			</ButtonBase>
			<Box flex={1}>
				<Button
					onPress={() => sheet.current?.present()}
					onLongPress={startDrag}
					justify="flex-start"
					leftSection={<IconLink size={IconSize.sm} color={Colors.TextDimmed} />}
				>
					<TransText value={editor.value.name} fallback={editor.value.url ?? "Unnamed Link"} />
				</Button>
			</Box>

			<Sheet ref={sheet}>
				<Box gap="md">
					<LinkEditSheetContent editor={editor} />
					<Box direction="row" gap="sm" justify="space-between">
						<Button
							variant="danger"
							leftSection={<IconTrash size={IconSize.sm} color={Colors.Text} />}
							onPress={() => {
								onDelete();
								sheet.current?.dismiss();
							}}
						>
							Delete
						</Button>
						<Button
							leftSection={<IconCheck size={IconSize.sm} color={Colors.Text} />}
							onPress={() => sheet.current?.dismiss()}
						>
							Done
						</Button>
					</Box>
				</Box>
			</Sheet>
		</Box>
	);
};
