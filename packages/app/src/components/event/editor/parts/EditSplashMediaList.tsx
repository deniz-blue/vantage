import { SplashMediaComponent } from "@evnt/types";
import { createEditor, Editor } from "../editor";
import { Box } from "../../../base/Box";
import { useMemo, useRef, useState } from "react";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { useReorderableDrag } from "react-native-reorderable-list";
import { Sheet, SheetRef } from "../../../base/sheet/Sheet";
import { FormComponents } from "../FormComponents";
import { Button } from "../../../base/button/Button";
import { IconCheck, IconPhoto, IconTrash } from "@tabler/icons-react-native";
import { IconSize } from "../../../../theme/sizing";
import { Colors } from "../../../../theme/colors";
import { TransText } from "../../../core/TransText";
import { EditSplashMedia } from "./EditSplashMedia";
import { ButtonSheet } from "../../../app/ButtonSheet";
import { useEventFormContext } from "../event-form-context";

const createDraftSplashMedia = (): SplashMediaComponent => ({
	$type: "directory.evnt.component.splashMedia",
	media: {
		sources: [],
	},
	roles: [],
});

export const EditSplashMediaList = () => {
	const { editor } = useEventFormContext();
	const addLinkSheet = useRef<SheetRef>(null);
	const [draft, setDraft] = useState<SplashMediaComponent>(createDraftSplashMedia());

	const components = useMemo(() => editor.field("components", []), [editor]);

	const onAddDraft = () => {
		components.push(draft);
		addLinkSheet.current?.dismiss();
		setDraft(createDraftSplashMedia());
	};

	return (
		<Box gap="xs">
			<Box direction="row" gap="sm" justify="space-between" align="center">
				<InputWrapper label="Splash Media" />
				<InputWrapper description="Poster, banner, etc." />
			</Box>

			<FormComponents<SplashMediaComponent>
				type="directory.evnt.component.splashMedia"
				renderItem={SplashMediaRow}
				editor={editor}
			/>

			<Sheet ref={addLinkSheet}>
				<Box gap="md">
					<EditSplashMedia editor={createEditor(draft, setDraft)} />

					<Button onPress={onAddDraft}>Add</Button>
				</Box>
			</Sheet>

			<Button onPress={() => addLinkSheet.current?.present()}>Add Splash Media</Button>
		</Box>
	);
};

export const SplashMediaRow = ({
	editor,
	onDelete,
}: {
	editor: Editor<SplashMediaComponent>;
	onDelete: () => void;
}) => {
	const startDrag = useReorderableDrag();

	return (
		<ButtonSheet
			sheet={(ref) => (
				<Box gap="md">
					<EditSplashMedia editor={editor} />
					<Box direction="row" gap="sm" justify="space-between">
						<Button
							variant="danger"
							leftSection={<IconTrash size={IconSize.sm} color={Colors.Text} />}
							onPress={() => {
								onDelete();
								ref.current?.dismiss();
							}}
						>
							Delete
						</Button>
						<Button
							leftSection={<IconCheck size={IconSize.sm} color={Colors.Text} />}
							onPress={() => ref.current?.dismiss()}
						>
							Done
						</Button>
					</Box>
				</Box>
			)}
			onLongPress={startDrag}
			justify="flex-start"
			leftSection={<IconPhoto size={IconSize.sm} color={Colors.TextDimmed} />}
			my="xs"
		>
			<TransText value={editor.value.media.alt} fallback={`Splash Media`} numberOfLines={1} />
		</ButtonSheet>
	);
};
