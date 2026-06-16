import { OpenEvnt } from "@evnt/types";
import { Box } from "../../base/Box";
import { Editor } from "./useEditor";
import { TranslationsInput } from "./input/TranslationsInput";
import { StatusPicker } from "./picker/StatusPicker";

export const EventForm = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	return (
		<Box gap="md">
			<TranslationsInput
				label="Event Name"
				editor={editor.field(e => e.name)}
			/>

			<StatusPicker
				value={editor.value.status || "planned"}
				onChange={status => editor.update(d => { d.status = status })}
			/>
		</Box>
	)
};
