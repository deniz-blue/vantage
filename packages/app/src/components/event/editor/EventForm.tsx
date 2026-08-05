import { EventInstance, OpenEvnt, Venue } from "@evnt/types";
import { Box } from "../../base/Box";
import { Editor } from "./editor";
import { TranslationsInput } from "./input/TranslationsInput";
import { StatusPicker } from "./picker/StatusPicker";
import { EventInstanceEditor } from "./EventInstanceEditor";
import { Text } from "../../base/Text";
import { Line } from "../../base/Divider";
import { Button } from "../../base/button/Button";
import { EventFormContext, useEventFormContext } from "./event-form-context";
import { Fragment, ReactNode, useCallback, useMemo, useRef } from "react";
import { Sheet, SheetRef } from "../../base/sheet/Sheet";
import { EventVenueEditor } from "./EventVenueEditor";
import { IconMapPin, IconPlus, IconQuestionMark, IconWorld } from "@tabler/icons-react-native";
import { FontSize, IconSize, Radius } from "../../../theme/sizing";
import { EventDescriptionEditor } from "./EventDescriptionEditor";
import { Colors } from "../../../theme/colors";
import { ButtonBase } from "../../base/ButtonBase";
import { EventFormLinks } from "./EventFormLinks";

export const EventForm = ({ editor }: { editor: Editor<OpenEvnt> }) => {
	const name = useMemo(() => editor.field("name"), [editor]);

	return (
		<EventFormContext value={{ editor }}>
			<Box gap="md">
				<TranslationsInput label="Event Name" placeholder="My Event" editor={name} />

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

				<Box direction="row" gap="sm" align="center">
					<Text c="TextDimmed" fw="bold">
						Details
					</Text>
					<Line />
				</Box>

				<EventDescriptionEditor editor={editor} />

				<EventFormLinks editor={editor} />
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
	const children = useMemo(() => {
		if (!editor.value?.length) return null;
		return editor.value.map((_, i) => (
			<Fragment key={i}>
				{renderItem({
					editor: editor.at(i),
					index: i,
					onDelete: () => editor.update((d) => void d.splice(i, 1)),
				})}
			</Fragment>
		));
	}, [editor, renderItem]);

	return (
		<Box gap="md">
			<Box direction="row" gap="sm" align="center">
				<Text c="TextDimmed" fw="bold">
					{title}
				</Text>
				<Line />
				<Button
					size="sm"
					onPress={onAdd}
					rightSection={<IconPlus size={IconSize.xs} color={Colors.Text} />}
				>
					Add
				</Button>
			</Box>

			{!editor.value?.length && (
				<Box align="center">
					<Text c="TextDimmed" fz={FontSize.sm}>
						{emptyText}
					</Text>
				</Box>
			)}

			{children}
		</Box>
	);
};

export const EventFormInstances = () => {
	const { editor } = useEventFormContext();

	const renderItem = useCallback(
		({ editor, onDelete }: { editor: Editor<EventInstance>; onDelete: () => void }) => (
			<EventInstanceEditor editor={editor} onDelete={onDelete} />
		),
		[],
	);

	const instances = useMemo(() => editor.field("instances", []), [editor]);

	const onAdd = useCallback(() => {
		instances.push({
			venueIds: [],
		});
	}, [instances]);

	return (
		<FormList
			title="Date & Time"
			emptyText="No dates set"
			editor={instances}
			onAdd={onAdd}
			renderItem={renderItem}
		/>
	);
};

export const EventFormVenues = () => {
	const { editor } = useEventFormContext();
	const sheet = useRef<SheetRef>(null);

	const onAdd = useCallback(
		(type: Venue["$type"]) => {
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
			sheet.current?.dismiss();
		},
		[editor],
	);

	const renderItem = useCallback(
		({ editor, onDelete }: { editor: Editor<Venue>; onDelete: () => void }) => (
			<EventVenueEditor editor={editor} onDelete={onDelete} />
		),
		[],
	);

	const venues = useMemo(() => editor.field("venues", []), [editor]);

	return (
		<Fragment>
			<FormList
				title="Locations"
				emptyText="No locations set"
				editor={venues}
				onAdd={() => sheet.current?.present()}
				renderItem={renderItem}
			/>

			<Sheet ref={sheet}>
				<Box gap="md" w="100%">
					<Text ta="center">Select location type:</Text>
					<Box direction="row" w="100%">
						{(
							[
								"directory.evnt.venue.online",
								"directory.evnt.venue.physical",
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
								<Box key={type} flex={type === "directory.evnt.venue.physical" ? 2 : 1}>
									<Box px="xs">
										<ButtonBase onPress={() => onAdd(type)}>
											<Box
												bg={Colors.BackgroundLight}
												radius={Radius.Default}
												py="sm"
												gap="xs"
												align="center"
												justify="center"
											>
												<Icon size={IconSize.lg} color={Colors.Text} />
												<Text fz={FontSize.sm}>{title}</Text>
											</Box>
										</ButtonBase>
									</Box>
								</Box>
							);
						})}
					</Box>
				</Box>
			</Sheet>
		</Fragment>
	);
};
