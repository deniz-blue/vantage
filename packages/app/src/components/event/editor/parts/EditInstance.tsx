import { EventInstance, Venue } from "@evnt/types";
import { formatDate } from "@evnt/pretty";
import { Box } from "../../../base/Box";
import { Editor } from "../editor";
import {
	IconCalendar,
	IconChevronRight,
	IconMapPin,
	IconProps,
	IconQuestionMark,
	IconWorld,
} from "@tabler/icons-react-native";
import { Colors } from "../../../../theme/colors";
import { Text } from "../../../base/Text";
import { FontSize, IconSize } from "../../../../theme/sizing";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { PartialDateInput } from "../input/PartialDateInput";
import { TransText } from "../../../core/TransText";
import { SelectItemProps } from "../../../base/input/Select";
import { MultiSelect } from "../../../base/input/MultiSelect";
import { useEventFormContext } from "../event-form-context";
import { useMemo } from "react";
import { InputWrapper } from "../../../base/input/InputWrapper";
import { Button } from "../../../base/button/Button";
import { ButtonSheet } from "../../../app/ButtonSheet";

export const EditInstanceRow = ({
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
		<ButtonSheet
			sheet={(ref) => (
				<EditInstance editor={editor} onDelete={onDelete} onClose={() => ref.current?.dismiss()} />
			)}
			leftSection={<IconCalendar size={IconSize.sm} color={Colors.TextDimmed} />}
			rightSection={<IconChevronRight size={IconSize.sm} color={Colors.TextDimmed} />}
		>
			<Text c="TextDimmed" fz={FontSize.sm}>
				{editor.value.start ? formatDate(editor.value.start, config) : "Unspecified Date"}
			</Text>
		</ButtonSheet>
	);
};

export const EditInstance = ({
	editor,
	onDelete,
	onClose,
}: {
	editor: Editor<EventInstance>;
	onDelete?: () => void;
	onClose?: () => void;
}) => {
	const userLanguage = useLocaleStore((s) => s.language);
	const userTimezone = useLocaleStore((s) => s.timezone);

	const config = {
		language: userLanguage,
		timezone: userTimezone,
		compactDates: true,
	};

	return (
		<Box gap="md">
			<Box direction="row">
				<Box flex={1} gap="xs" direction="row">
					<IconCalendar size={IconSize.xs} color={Colors.TextDimmed} />
					<Text c="TextDimmed" fz={FontSize.sm}>
						{editor.value.start ? formatDate(editor.value.start, config) : "Unspecified Date"}
					</Text>
				</Box>
			</Box>

			<Box direction="row" gap="sm">
				<Box flex={1}>
					<PartialDateInput
						label="Start"
						value={editor.value.start}
						onChange={(value) =>
							editor.update((d) => {
								d.start = value;
							})
						}
					/>
				</Box>
				<Box flex={1}>
					<PartialDateInput
						disabled={!editor.value.start}
						label="End"
						value={editor.value.end}
						onChange={(value) =>
							editor.update((d) => {
								d.end = value;
							})
						}
					/>
				</Box>
			</Box>

			<EventInstanceEditorVenueIds editor={editor} />

			<Box direction="row" gap="sm" justify="space-between">
				<Button variant="danger" onPress={onDelete}>
					Delete
				</Button>
				<Button onPress={onClose}>Done</Button>
			</Box>
		</Box>
	);
};

export const EventInstanceEditorVenueIds = ({ editor }: { editor: Editor<EventInstance> }) => {
	const { editor: root } = useEventFormContext();

	const allVenues = useMemo(() => root.value.venues ?? [], [root.value.venues]);

	const venuesOfInstance = useMemo(
		() => allVenues.filter((v) => editor.value.venueIds.includes(v.id)),
		[allVenues, editor.value.venueIds],
	);

	const isAllSelected = allVenues.length === venuesOfInstance.length;

	return (
		<Box>
			<InputWrapper label="Locations" />
			<Box direction="row" gap="sm" align="center">
				{!isAllSelected && (
					<Box flex={1}>
						<Button
							onPress={() => {
								editor.update((d) => {
									d.venueIds = allVenues.map((v) => v.id);
								});
							}}
						>
							Select All ({venuesOfInstance.length}/{allVenues.length})
						</Button>
					</Box>
				)}
				<Box flex={1}>
					<MultiSelect
						data={allVenues}
						value={venuesOfInstance}
						renderItem={EventVenueItem}
						disabled={!allVenues.length}
						buttonContent={<Text fz={FontSize.sm}>{venuesOfInstance.length} locations</Text>}
						onChange={(venues) => {
							editor.update((d) => {
								d.venueIds = venues.map((v) => v.id);
							});
						}}
					/>
				</Box>
			</Box>
		</Box>
	);
};

export const EventVenueItem = ({ value }: SelectItemProps<Venue>) => {
	const Icon =
		(
			{
				"directory.evnt.venue.online": IconWorld,
				"directory.evnt.venue.physical": IconMapPin,
				"directory.evnt.venue.unknown": IconQuestionMark,
			} as Record<Venue["$type"], React.ComponentType<IconProps>>
		)[value.$type] ?? IconQuestionMark;

	return (
		<Box direction="row" gap="sm" align="center">
			<Icon size={IconSize.sm} color={Colors.Text} />
			<TransText value={value.name} />
		</Box>
	);
};
