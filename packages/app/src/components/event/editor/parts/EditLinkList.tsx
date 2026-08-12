import { Box } from "../../../base/Box";
import { Button } from "../../../base/button/Button";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { useReorderableDrag } from "react-native-reorderable-list";
import { Sheet, SheetRef } from "../../../base/sheet/Sheet";
import { useMemo, useRef, useState } from "react";
import { createEditor, Editor } from "../editor";
import { LinkComponent } from "@evnt/types";
import { EditLink } from "./EditLink";
import { IconCheck, IconGripHorizontal, IconLink, IconTrash } from "@tabler/icons-react-native";
import { Colors } from "../../../../theme/colors";
import { IconSize } from "../../../../theme/sizing";
import { ButtonBase } from "../../../base/ButtonBase";
import { TransText } from "../../../core/TransText";
import { FormComponents } from "../FormComponents";
import { useEventFormContext } from "../event-form-context";

const createDraftLink = (): LinkComponent => ({
	$type: "directory.evnt.component.link",
	url: "",
});

export const EditLinkList = () => {
	const { editor } = useEventFormContext();
	const addLinkSheet = useRef<SheetRef>(null);
	const [draftLink, setDraftLink] = useState<LinkComponent>(createDraftLink());

	const components = useMemo(() => editor.field("components", []), [editor]);

	const onAddDraftLink = () => {
		components.push(draftLink);
		addLinkSheet.current?.dismiss();
		setDraftLink(createDraftLink());
	};

	return (
		<Box gap="xs">
			<Box direction="row" gap="sm" justify="space-between" align="center">
				<InputWrapper label="Links" />
				<InputWrapper description="Long press to reorder" />
			</Box>

			<FormComponents<LinkComponent>
				type="directory.evnt.component.link"
				renderItem={EditLinkRow}
				editor={editor}
			/>

			<Sheet ref={addLinkSheet}>
				<Box gap="md">
					<EditLink editor={createEditor(draftLink, setDraftLink)} />

					<Button onPress={onAddDraftLink} disabled={!draftLink.url.trim()}>
						Add Link
					</Button>
				</Box>
			</Sheet>

			<Button onPress={() => addLinkSheet.current?.present()}>Add Link</Button>
		</Box>
	);
};

export const EditLinkRow = ({
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
					<TransText
						value={editor.value.name}
						fallback={editor.value.url ?? "Unnamed Link"}
						numberOfLines={1}
					/>
				</Button>
			</Box>

			<Sheet ref={sheet}>
				<Box gap="md">
					<EditLink editor={editor} />
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
