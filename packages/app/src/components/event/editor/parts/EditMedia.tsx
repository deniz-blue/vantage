import { Media } from "@evnt/types";
import { Colors } from "../../../../theme/colors";
import { Box } from "../../../base/Box";
import { Button } from "../../../base/button/Button";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { Text } from "../../../base/Text";
import { OpenEvntImage } from "../../EventBackground";
import { Editor } from "../editor";
import { EditorList } from "../FormList";
import { TranslationsInput } from "../input/TranslationsInput";
import { EditMediaSource } from "./EditMediaSource";
import { useMemo } from "react";

export const EditMedia = ({ editor }: { editor: Editor<Media> }) => {
	const sources = useMemo(() => editor.field("sources", []), [editor]);

	return (
		<Box gap="md">
			<InputWrapper label="Preview" />

			<Box h={100} bg={Colors.BackgroundLight}>
				<Box absoluteFill justify="center" align="center">
					<Text c="TextDimmed" ta="center">
						Preview will show here
					</Text>
				</Box>
				<OpenEvntImage media={editor.value} absoluteFill />
			</Box>

			<Box gap="sm">
				<InputWrapper label="Sources" />
				<EditorList editor={sources} renderItem={EditMediaSource} />
				<Button onPress={() => sources.push({ url: "" })}>Add Image URL</Button>
			</Box>

			<TranslationsInput label="Alt Text" editor={editor.field("alt", {})} />
		</Box>
	);
};
