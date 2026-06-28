import { OpenEvnt, Venue } from "@evnt/types";
import { Box } from "../../base/Box";
import { Editor } from "./useEditor";
import { TranslationsInput } from "./input/TranslationsInput";
import { StatusPicker } from "./picker/StatusPicker";
import { EventInstanceEditor } from "./EventInstanceEditor";
import { Text } from "../../base/Text";
import { Divider } from "../../base/Divider";
import { Button } from "../../base/Button";
import { EventFormContext, useEventFormContext } from "./event-form-context";
import { useState } from "react";
import { Sheet } from "../../base/Sheet";
import { EventVenueEditor } from "./EventVenueEditor";
import { IconPlus } from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { FontSize, IconSize } from "../../../theme/sizing";
import { EventDescriptionEditor } from "./EventDescriptionEditor";

export const EventForm = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	return (
		<EventFormContext value={{ editor }}>
			<Box gap="md">
				<TranslationsInput
					label="Event Name"
					placeholder="My Event"
					editor={editor.field(e => e.name)}
				/>

				<StatusPicker
					value={editor.value.status || "planned"}
					onChange={status => editor.update(d => { d.status = status })}
				/>

				<EventFormInstances />
				<EventFormVenues />

				<Divider
					leftSection={<Text c="TextDimmed" fw="600">Details</Text>}
				/>

				<EventDescriptionEditor editor={editor} />
			</Box>
		</EventFormContext>
	)
};

export const EventFormInstances = () => {
	const { editor } = useEventFormContext();

	return (
		<Box gap="md">
			<Divider
				leftSection={<Text c="TextDimmed" fw="600">Date & Time</Text>}
				rightSection={(
					<Button
						variant="subtle"
						onPress={() => editor.update(d => {
							if (!d.instances) d.instances = [];
							d.instances.push({
								venueIds: [],
							});
						})}
						rightSection={<IconPlus color={Colors.Primary} size={IconSize.xs} />}
					>
						Add
					</Button>
				)}
			/>

			{!editor.value.instances?.length && (
				<Box align="center">
					<Text c="TextDimmed" fz={FontSize.sm}>
						No dates.
					</Text>
				</Box>
			)}

			{editor.value.instances?.map((_, i) => (
				<EventInstanceEditor
					key={i}
					editor={editor.field(e => e.instances![i]!)}
					onDelete={() => editor.update(d => void d.instances!.splice(i, 1))}
				/>
			))}
		</Box>
	);
};

export const EventFormVenues = () => {
	const { editor } = useEventFormContext();
	const [open, setOpen] = useState(false);

	const onAdd = (type: Venue["$type"]) => {
		editor.update(d => {
			if (!d.venues) d.venues = [];
			let nextId = 0;
			while (d.venues.some(v => v.id === nextId.toString())) nextId++;
			d.venues.push({
				$type: type,
				id: nextId.toString(),
				name: {},
			});
		});
		setOpen(false);
	};

	return (
		<Box gap="md">
			<Divider
				leftSection={<Text c="TextDimmed" fw="600">Location</Text>}
				rightSection={(
					<Button
						variant="subtle"
						onPress={() => setOpen(true)}
						rightSection={<IconPlus color={Colors.Primary} size={IconSize.xs} />}
					>
						Add
					</Button>
				)}
			/>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box p="md" gap="md" flex={1}>
					<Button size="md" onPress={() => onAdd("directory.evnt.venue.physical")}>
						Add Physical Location
					</Button>
					<Button size="md" onPress={() => onAdd("directory.evnt.venue.online")}>
						Add Virtual Location
					</Button>
					<Button size="md" onPress={() => onAdd("directory.evnt.venue.unknown")}>
						Add Generic Location
					</Button>
				</Box>
			</Sheet>

			{!editor.value.venues?.length && (
				<Box align="center">
					<Text c="TextDimmed" fz={FontSize.sm}>
						No locations.
					</Text>
				</Box>
			)}

			{editor.value.venues?.map((_, i) => (
				<EventVenueEditor
					key={i}
					editor={editor.field(e => e.venues![i]!)}
					onDelete={() => editor.update(d => void d.venues!.splice(i, 1))}
				/>
			))}
		</Box>
	);
};
