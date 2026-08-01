import { OpenEvnt } from "@evnt/types";
import { TextInput } from "../../base/input/TextInput";
import { Editor } from "./editor";
import { Box } from "../../base/Box";
import { Button } from "../../base/button/Button";
import { Sheet, SheetRef } from "../../base/sheet/Sheet";
import { useRef } from "react";
import { MarkdownRichtext } from "../../core/richtext/MarkdownRichtext";
import { Text } from "../../base/Text";
import { Line } from "../../base/Divider";

export const EventDescriptionEditor = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	const sheet = useRef<SheetRef>(null);

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

			<Button onPress={() => sheet.current?.present()} disabled={!value.trim()}>
				Preview
			</Button>

			<Sheet ref={sheet}>
				<Box gap="md">
					<Box direction="row" gap="sm" align="center">
						<Line />
						<Text fw="bold">Description Preview</Text>
						<Line />
					</Box>
					<MarkdownRichtext content={value} />
					<Box h={100} />
				</Box>
			</Sheet>
		</Box>
	);
};
