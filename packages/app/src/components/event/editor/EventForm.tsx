import { OpenEvnt, Venue } from "@evnt/types";
import { Box } from "../../base/Box";
import { Editor } from "./editor";
import { TranslationsInput } from "./input/TranslationsInput";
import { StatusPicker } from "./picker/StatusPicker";
import { EventInstanceEditor } from "./EventInstanceEditor";
import { Text } from "../../base/Text";
import { Divider } from "../../base/Divider";
import { Button } from "../../base/button/Button";
import { EventFormContext, useEventFormContext } from "./event-form-context";
import { Fragment, ReactNode, useState } from "react";
import { Sheet } from "../../base/sheet/Sheet";
import { EventVenueEditor } from "./EventVenueEditor";
import { IconMapPin, IconPlus, IconQuestionMark, IconWorld } from "@tabler/icons-react-native";
import { FontSize, IconSize } from "../../../theme/sizing";
import { EventDescriptionEditor } from "./EventDescriptionEditor";
import { ActionIcon } from "../../base/button/ActionIcon";
import { Colors } from "../../../theme/colors";

export const EventForm = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	return (
		<EventFormContext value={{ editor }}>
			<Box gap="md">
				<TranslationsInput
					label="Event Name"
					placeholder="My Event"
					editor={editor.field((e) => e.name)}
				/>

				<StatusPicker
					value={editor.value.status || "planned"}
					onChange={(status) =>
						editor.update((d) => {
							d.status = status;
						})
					}
				/>

				<EventFormInstances />
				<EventFormVenues />

				<Divider
					leftSection={
						<Text c="TextDimmed" fw="600">
							Details
						</Text>
					}
				/>

				<EventDescriptionEditor editor={editor} />
			</Box>
		</EventFormContext>
	);
};

export const FormList = <T,>({
	title,
	editor,
	onAdd,
	emptyText,
	renderItem,
}: {
	title: string;
	editor: Editor<T[] | undefined>;
	onAdd: () => void;
	emptyText: ReactNode;
	renderItem: (props: { onDelete: () => void; editor: Editor<T>; index: number }) => ReactNode;
}) => {
	return (
		<Box gap="md">
			<Divider
				leftSection={
					<Text c="TextDimmed" fw="600">
						{title}
					</Text>
				}
				rightSection={
					<Button
						variant="subtle"
						onPress={onAdd}
						rightSection={<IconPlus size={IconSize.xs} color={Colors.Primary} />}
					>
						<Text fz={FontSize.sm} c={Colors.Primary}>
							Add
						</Text>
					</Button>
				}
			/>

			{!editor.value?.length && (
				<Box align="center">
					<Text c="TextDimmed" fz={FontSize.sm}>
						{emptyText}
					</Text>
				</Box>
			)}

			{editor.value?.map((_, i) => (
				<Fragment key={i}>
					{renderItem({
						editor: editor.field((v) => v![i]!),
						index: i,
						onDelete: () => editor.update((d) => void d?.splice(i, 1)),
					})}
				</Fragment>
			))}
		</Box>
	);
};

export const EventFormInstances = () => {
	const { editor } = useEventFormContext();

	return (
		<FormList
			title="Date & Time"
			emptyText="No dates set"
			editor={editor.field((e) => e.instances)}
			onAdd={() => {
				editor.update((d) => {
					if (!d.instances) d.instances = [];
					d.instances.push({
						venueIds: [],
					});
				});
			}}
			renderItem={({ editor, onDelete }) => (
				<EventInstanceEditor editor={editor} onDelete={onDelete} />
			)}
		/>
	);
};

export const EventFormVenues = () => {
	const { editor } = useEventFormContext();
	const [open, setOpen] = useState(false);

	const onAdd = (type: Venue["$type"]) => {
		editor.update((d) => {
			if (!d.venues) d.venues = [];
			let nextId = 0;
			while (d.venues.some((v) => v.id === nextId.toString())) nextId++;
			d.venues.push({
				$type: type,
				id: nextId.toString(),
				name: {},
			});
		});
		setOpen(false);
	};

	return (
		<Fragment>
			<FormList
				title="Locations"
				emptyText="No locations set"
				editor={editor.field((e) => e.venues)}
				onAdd={() => setOpen(true)}
				renderItem={({ editor, onDelete }) => (
					<EventVenueEditor editor={editor} onDelete={onDelete} />
				)}
			/>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<Box gap="md">
					<Text ta="center">Select location type:</Text>
					<Box direction="row" gap="sm" flex={1}>
						{(
							[
								"directory.evnt.venue.physical",
								"directory.evnt.venue.online",
								"directory.evnt.venue.unknown",
							] as const
						).map((type) => {
							let title = type.slice("directory.evnt.venue.".length);
							title = title[0].toUpperCase() + title.slice(1);

							const Icon =
								{
									Physical: IconMapPin,
									Online: IconWorld,
									Unknown: IconQuestionMark,
								}[title] ?? Fragment;

							return (
								<Button flex={1} key={type} onPress={() => onAdd(type)}>
									<Box py="sm" gap="xs" align="center" flex={1}>
										<ActionIcon size="lg">
											<Icon size={IconSize.lg} color={Colors.Text} />
										</ActionIcon>

										<Text fz={FontSize.sm}>{title}</Text>
									</Box>
								</Button>
							);
						})}
					</Box>
				</Box>
			</Sheet>
		</Fragment>
	);
};
