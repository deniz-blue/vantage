import { Venue } from "@evnt/types";
import { Editor } from "../editor";
import { Box } from "../../../base/Box";
import { TranslationsInput } from "../input/TranslationsInput";
import { ComponentType } from "react";
import {
	IconChevronRight,
	IconMap2,
	IconMapPin,
	IconProps,
	IconWorld,
} from "@tabler/icons-react-native";
import { Text } from "../../../base/Text";
import { FontSize, IconSize } from "../../../../theme/sizing";
import { Colors } from "../../../../theme/colors";
import { TextInput } from "../../../base/input/TextInput";
import { CountrySelect } from "../input/CountrySelect";
import { ButtonSheet } from "../../../app/ButtonSheet";
import { TransText } from "../../../core/TransText";
import { Button } from "../../../base/button/Button";

const ICONS: Record<Venue["$type"], ComponentType<IconProps>> = {
	"directory.evnt.venue.physical": IconMapPin,
	"directory.evnt.venue.online": IconWorld,
	"directory.evnt.venue.unknown": IconMap2,
};

const LABELS: Record<Venue["$type"], string> = {
	"directory.evnt.venue.physical": "Physical Location",
	"directory.evnt.venue.online": "Virtual Location",
	"directory.evnt.venue.unknown": "Location",
};

export const EditVenueRow = ({
	editor,
	onDelete,
}: {
	editor: Editor<Venue>;
	onDelete: () => void;
}) => {
	const Icon = ICONS[editor.value.$type] || ICONS["directory.evnt.venue.unknown"];
	const label = LABELS[editor.value.$type] || LABELS["directory.evnt.venue.unknown"];

	return (
		<ButtonSheet
			sheet={(ref) => (
				<EditVenue editor={editor} onDelete={onDelete} onClose={() => ref.current?.dismiss()} />
			)}
			leftSection={<Icon size={IconSize.sm} color={Colors.TextDimmed} />}
			rightSection={<IconChevronRight size={IconSize.sm} color={Colors.TextDimmed} />}
		>
			<TransText fz={FontSize.sm} value={editor.value.name} fallback={label} numberOfLines={1} />
		</ButtonSheet>
	);
};

export const EditVenue = ({
	editor,
	onDelete,
	onClose,
}: {
	editor: Editor<Venue>;
	onDelete: () => void;
	onClose?: () => void;
}) => {
	const Icon = ICONS[editor.value.$type] || ICONS["directory.evnt.venue.unknown"];
	const label = LABELS[editor.value.$type] || LABELS["directory.evnt.venue.unknown"];

	return (
		<Box gap="md">
			<Box direction="row">
				<Box flex={1}>
					<Box direction="row" align="center" gap="xs">
						<Icon size={IconSize.xs} color={Colors.TextDimmed} />
						<Text c="TextDimmed" fz={FontSize.sm}>
							{label}
						</Text>
					</Box>
				</Box>
			</Box>

			<TranslationsInput label="Name" placeholder="Triangle Bar" editor={editor.field("name")} />

			{editor.value.$type === "directory.evnt.venue.physical" && (
				<Box direction="row" gap="xs">
					<Box flex={1}>
						<TextInput
							label="Address"
							placeholder="123 Main Street"
							value={editor.value.address?.addr ?? ""}
							onChangeText={(text) =>
								editor.update((d) => {
									if (d.$type !== "directory.evnt.venue.physical") return;
									d.address ??= {};
									d.address.addr = text;
								})
							}
						/>
					</Box>
					<CountrySelect
						value={editor.value.address?.countryCode}
						onChange={(countryCode) =>
							editor.update((d) => {
								if (d.$type !== "directory.evnt.venue.physical") return;
								d.address ??= {};
								d.address.countryCode = countryCode;
							})
						}
					/>
				</Box>
			)}

			<Box direction="row" gap="sm" justify="space-between">
				<Button variant="danger" onPress={onDelete}>
					Delete
				</Button>
				<Button onPress={onClose}>Done</Button>
			</Box>
		</Box>
	);
};
