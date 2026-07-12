import { EventInstance } from "@evnt/types";
import { formatDate } from "@evnt/pretty";
import { Box } from "../../base/Box";
import { Editor } from "./editor";
import { Card } from "../../base/Card";
import { IconCalendar } from "@tabler/icons-react-native";
import { CloseButton } from "../../base/button/CloseButton";
import { Colors } from "../../../theme/colors";
import { Text } from "../../base/Text";
import { FontSize, IconSize } from "../../../theme/sizing";
import { useLocaleStore } from "../../../stores/useLocaleStore";
import { PartialDateInput } from "./input/PartialDateInput";

export const EventInstanceEditor = ({
	editor,
	onDelete,
}: {
	editor: Editor<EventInstance>;
	onDelete?: () => void;
}) => {
	const userLanguage = useLocaleStore((s) => s.language);
	const userTimezone = useLocaleStore((s) => s.timezone);

	const config = {
		language: userLanguage,
		timezone: userTimezone,
		compactDates: true,
	};

	return (
		<Card>
			<Box gap="md">
				<Box direction="row">
					<Box flex={1} gap="xs" direction="row">
						<IconCalendar size={IconSize.xs} color={Colors.TextDimmed} />
						<Text c="TextDimmed" fz={FontSize.sm}>
							{editor.value.start ? formatDate(editor.value.start, config) : "Unspecified Date"}
						</Text>
					</Box>
					<Box>
						<CloseButton onPress={onDelete} />
					</Box>
				</Box>

				<PartialDateInput
					label="Start"
					value={editor.value.start}
					onChange={(value) =>
						editor.update((d) => {
							d.start = value;
						})
					}
				/>

				<PartialDateInput
					label="End"
					value={editor.value.end}
					onChange={(value) =>
						editor.update((d) => {
							d.end = value;
						})
					}
				/>
			</Box>
		</Card>
	);
};
