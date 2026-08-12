import { MediaSource } from "@evnt/types";
import { Editor } from "../editor";
import { Box } from "../../../base/Box";
import { TextInput } from "../../../base/input/TextInput";
import { ActionIcon } from "../../../base/button/ActionIcon";
import { IconSize } from "../../../../theme/sizing";
import { IconX } from "@tabler/icons-react-native";
import { Colors } from "../../../../theme/colors";

export const EditMediaSource = ({
	editor,
	onDelete,
}: {
	editor: Editor<MediaSource>;
	onDelete: () => void;
}) => {
	return (
		<Box direction="row" gap="sm" align="center">
			<Box flex={1}>
				<TextInput
					placeholder="https://example.com/image.jpg"
					value={editor.value.url}
					onChangeText={(url) => editor.update((d) => (d.url = url))}
				/>
			</Box>
			<ActionIcon onPress={onDelete}>
				<IconX size={IconSize.sm} color={Colors.Text} />
			</ActionIcon>
		</Box>
	);
};
