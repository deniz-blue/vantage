import { OpenEvnt } from "@evnt/types";
import { TextInput } from "../../base/input/TextInput";
import { Editor } from "./editor";

export const EventDescriptionEditor = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	const md: any = editor.value.components?.find(c => c.$type === "directory.evnt.richtext.markdown");
	const value = (md?.markdown as string | undefined) ?? "";

	return (
		<TextInput
			label="Event Description"
			placeholder={"Add a description...\nSupports Markdown formatting"}
			value={value}
			multiline
			textAlignVertical="top"
			baseProps={{ style: { height: 100 } }}
			onChangeText={(text) => {
				editor.update(d => {
					if (!d.components) d.components = [];
					let md = d.components.find(c => c.$type === "directory.evnt.richtext.markdown");
					if (!md && !text) return;
					if (!md) {
						md = {
							$type: "directory.evnt.richtext.markdown",
							markdown: text,
						};
						d.components.push(md);
					} else if (!text) {
						d.components = d.components.filter(c => c !== md);
					} else {
						(md as any).markdown = text;
					}
				});
			}}
		/>
	);
};
