import { OpenEvnt } from "@evnt/types";
import { TextInput } from "../../base/input/TextInput";
import { Editor } from "./editor";
import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { Sheet } from "../../base/sheet/Sheet";
import { useState } from "react";
import { MarkdownRichtext } from "../../core/richtext/MarkdownRichtext";
import { Text } from "../../base/Text";

export const EventDescriptionEditor = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	const [preview, setPreview] = useState(false);

	const md: any = editor.value.components?.find(
		(c) => c.$type === "directory.evnt.richtext.markdown",
	);
	const value = (md?.content as string | undefined) ?? "";

	const onChangeText = (text: string) => {
		editor.update((d) => {
			if (!d.components) d.components = [];
			let md = d.components.find((c) => c.$type === "directory.evnt.richtext.markdown");
			if (!md && !text) return;
			if (!md) {
				md = {
					$type: "directory.evnt.richtext.markdown",
					content: text,
					flavor: "gfm",
				};
				d.components.push(md);
			} else if (!text) {
				d.components = d.components.filter((c) => c !== md);
			} else {
				(md as any).content = text;
			}
		});
	};

	return (
		<Box gap="sm">
			<TextInput
				label="Event Description"
				placeholder={"Add a description...\nSupports Markdown formatting"}
				value={value}
				multiline
				textAlignVertical="top"
				baseProps={{ style: { height: 100 } }}
				onChangeText={onChangeText}
			/>

			<Button onPress={() => setPreview(true)} disabled={!value.trim()}>
				Preview
			</Button>

			<Sheet open={preview} onClose={() => setPreview(false)}>
				<Box gap="md">
					<Text fw="bold">Description Preview</Text>
					<MarkdownRichtext content={value} />
				</Box>
			</Sheet>
		</Box>
	);
};
