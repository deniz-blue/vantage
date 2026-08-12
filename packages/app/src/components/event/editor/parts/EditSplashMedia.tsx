import { SplashMediaComponent } from "@evnt/types";
import { Editor } from "../editor";
import { Box } from "../../../base/Box";
import { EditMedia } from "./EditMedia";

export const EditSplashMedia = ({ editor }: { editor: Editor<SplashMediaComponent> }) => {
	return (
		<Box gap="md">
			<EditMedia editor={editor.field("media", { sources: [] })} />
		</Box>
	);
};
