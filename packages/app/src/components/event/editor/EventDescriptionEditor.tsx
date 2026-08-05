import { OpenEvnt } from "@evnt/types";
import { TextInput } from "../../base/input/TextInput";
import { Editor } from "./editor";
import { Box } from "../../base/Box";
import { Sheet, SheetRef } from "../../base/sheet/Sheet";
import { useRef, useState } from "react";
import { MarkdownRichtext } from "../../core/richtext/MarkdownRichtext";
import { Text } from "../../base/Text";
import { Line } from "../../base/Divider";
import { InputWrapper } from "../../base/input/InputWrapper";
import { ButtonBase } from "../../base/ButtonBase";
import { FontSize } from "../../../theme/sizing";

export const EventDescriptionEditor = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	const sheet = useRef<SheetRef>(null);
	const [expanded, setExpanded] = useState(false);

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
		<Box gap="xs">
			<Box direction="row" gap="sm" justify="space-between" align="center">
				<InputWrapper label="Markdown Description" />
				<ButtonBase onPress={() => sheet.current?.present()} disabled={!value.trim()}>
					{value.trim() ? (
						<Text fz={FontSize.sm} c="Blue">
							Preview
						</Text>
					) : null}
				</ButtonBase>
			</Box>

			<TextInput
				placeholder={"Add a description"}
				value={value}
				multiline
				textAlignVertical="top"
				baseProps={{ style: { height: expanded ? 400 : 100 } }}
				onChangeText={onChangeText}
			/>

			<ButtonBase onPress={() => setExpanded((e) => !e)} disabled={!value.trim()}>
				{value.trim() ? (
					<Text fz={FontSize.sm} c="Blue">
						{expanded ? "Collapse" : "Expand"}
					</Text>
				) : null}
			</ButtonBase>

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
