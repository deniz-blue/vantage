import { Translations } from "@evnt/types";
import { Editor } from "../editor";
import { InputWrapper, InputWrapperProps } from "../../../base/input/InputWrapper";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { TextInput } from "../../../base/input/TextInput";
import { ActionIcon } from "../../../base/button/ActionIcon";
import { Colors } from "../../../../theme/colors";
import { IconLanguage } from "@tabler/icons-react-native";
import { useTranslator } from "../../../../hooks/useTranslator";

export const TranslationsInput = ({
	editor,
	placeholder,
	...props
}: InputWrapperProps & {
	editor: Editor<Translations>;
	placeholder?: string;
}) => {
	const userLanguage = useLocaleStore((s) => s.language);
	const t = useTranslator();

	return (
		<InputWrapper {...props}>
			<TextInput
				value={editor.value[userLanguage] || ""}
				onChangeText={(text) =>
					editor.update((d) => {
						d[userLanguage] = text;
					})
				}
				placeholder={t(editor.value) || placeholder || "Translation..."}
				rightSection={
					<ActionIcon variant="light" size="sm">
						<IconLanguage size={20} color={Colors.Text} />
					</ActionIcon>
				}
			/>
		</InputWrapper>
	);
};
