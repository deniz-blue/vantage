import { Translations } from "@evnt/types";
import { Editor } from "../useEditor";
import { InputWrapper, InputWrapperProps } from "../../../base/InputWrapper";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { TextInput } from "../../../base/TextInput";

export const TranslationsInput = ({
	editor,
	...props
}: InputWrapperProps & {
	editor: Editor<Translations>;
}) => {
	const userLanguage = useLocaleStore((s) => s.language);

	return (
		<InputWrapper
			{...props}
		>
			<TextInput
				value={editor.value[userLanguage] || ""}
				onChangeText={text => editor.update(d => { d[userLanguage] = text })}
			/>
		</InputWrapper>
	);
};
