import { PhysicalVenue, Venue } from "@evnt/types";
import { Editor } from "./useEditor";
import { Card } from "../../base/Card";
import { Box } from "../../base/Box";
import { CloseButton } from "../../base/CloseButton";
import { TranslationsInput } from "./input/TranslationsInput";
import { ComponentType } from "react";
import { IconGlobe, IconMap2, IconMapPin, IconProps, IconWorld } from "@tabler/icons-react-native";
import { Text } from "../../base/Text";
import { FontSize, IconSize } from "../../../theme/sizing";
import { Colors } from "../../../theme/colors";
import { TextInput } from "../../base/TextInput";

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

export const EventVenueEditor = ({
	editor,
	onDelete,
}: {
	editor: Editor<Venue>;
	onDelete: () => void;
}) => {
	const Icon = ICONS[editor.value.$type] || ICONS["directory.evnt.venue.unknown"];
	const label = LABELS[editor.value.$type] || LABELS["directory.evnt.venue.unknown"];

	return (
		<Card>
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
					<Box>
						<CloseButton onPress={onDelete} />
					</Box>
				</Box>

				<TranslationsInput
					label="Name"
					placeholder="Somewhere"
					editor={editor.field(e => e.name)}
				/>

				{editor.value.$type === "directory.evnt.venue.physical" && (
					<TextInput
						label="Address"
						placeholder="123 Main Street"
						value={editor.value.address?.addr ?? ""}
						onChangeText={text => editor.update(d => {
							if (d.$type !== "directory.evnt.venue.physical") return;
							d.address ??= {};
							d.address.addr = text;
						})}
					/>
				)}
			</Box>
		</Card>
	);
};
