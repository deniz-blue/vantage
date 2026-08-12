import { useMemo, useState } from "react";
import { Box } from "../../../base/Box";
import { TranslationsUtil } from "@evnt/translations";
import { useEventFormContext } from "../event-form-context";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { InlineTextButton } from "../../../base/button/InlineTextButton";
import { TranslationsInput } from "../input/TranslationsInput";
import { EventDescriptionEditor } from "../EventDescriptionEditor";
import { StatusPicker } from "../input/StatusPicker";
import { BaseTab } from "../BaseTab";

export const TabMain = () => {
	const { editor } = useEventFormContext();

	const [showLabel, setShowLabel] = useState(false);
	const hasLabel = !TranslationsUtil.isEmpty(editor.value.label);
	const name = useMemo(() => editor.field("name"), [editor]);
	const label = useMemo(() => editor.field("label"), [editor]);

	return (
		<BaseTab>
			<Box>
				<Box direction="row" justify="space-between">
					<InputWrapper label="Event Name" />
					{!hasLabel && (
						<InlineTextButton
							onPress={() => setShowLabel((s) => !s)}
							children={showLabel ? "[-]" : "[+]"}
						/>
					)}
				</Box>
				<TranslationsInput placeholder="My Event" editor={name} />
			</Box>

			{(showLabel || hasLabel) && (
				<TranslationsInput label="Subtitle" placeholder="Distinguishing Feature" editor={label} />
			)}

			<StatusPicker
				value={editor.value.status || "planned"}
				onChange={(status) =>
					editor.update((d) => {
						d.status = status;
					})
				}
			/>

			<EventDescriptionEditor editor={editor} />
		</BaseTab>
	);
};
