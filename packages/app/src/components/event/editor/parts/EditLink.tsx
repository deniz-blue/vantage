import { LinkComponent } from "@evnt/types";
import { Box } from "../../../base/Box";
import { Editor } from "../editor";
import { TextInput } from "../../../base/input/TextInput";
import { TranslationsInput } from "../input/TranslationsInput";

export const EditLink = ({ editor }: { editor: Editor<LinkComponent> }) => {
	return (
		<Box gap="md">
			<TextInput
				label="Link URL"
				value={editor.value.url}
				placeholder="https://event.com/tickets"
				onChangeText={(text) =>
					editor.update((d) => {
						d.url = text;
					})
				}
			/>

			<TranslationsInput
				placeholder="Buy Tickets"
				label="Link Name"
				editor={editor.field("name")}
			/>
		</Box>
	);
};
