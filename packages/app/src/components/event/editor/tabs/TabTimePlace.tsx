import { Fragment, useCallback, useMemo, useRef, useState } from "react";
import { Box } from "../../../base/Box";
import { useEventFormContext } from "../event-form-context";
import { Editor } from "../editor";
import { EventInstance, PartialDate, Venue } from "@evnt/types";
import { EditInstanceRow } from "../parts/EditInstance";
import { PartialDateUtil } from "@evnt/partial-date";
import { FormSectionList } from "../FormList";
import { Sheet, SheetRef } from "../../../base/sheet/Sheet";
import { EditVenueRow } from "../parts/EditVenue";
import { Text } from "../../../base/Text";
import {
	IconMapPin,
	IconQuestionMark,
	IconSquare,
	IconSquareCheck,
	IconWorld,
} from "@tabler/icons-react-native";
import { ButtonBase } from "../../../base/ButtonBase";
import { Colors } from "../../../../theme/colors";
import { FontSize, IconSize, Radius } from "../../../../theme/sizing";
import { Button } from "../../../base/button/Button";
import { BaseTab } from "../BaseTab";

export const TabTimePlace = () => {
	return (
		<BaseTab>
			<EventFormInstances />
			<EventFormVenues />
		</BaseTab>
	);
};

export const EventFormInstances = () => {
	const { editor } = useEventFormContext();

	const renderItem = useCallback(
		({ editor, onDelete }: { editor: Editor<EventInstance>; onDelete: () => void }) => (
			<EditInstanceRow editor={editor} onDelete={onDelete} />
		),
		[],
	);

	const instances = useMemo(() => editor.field("instances", []), [editor]);

	const onAdd = useCallback(() => {
		const last = instances.value?.[instances.value?.length - 1];
		let start: PartialDate | undefined = undefined;
		if (last?.start && PartialDateUtil.has(last.start, "day")) {
			if (PartialDateUtil.has(last.start, "time")) {
				let datetime = PartialDateUtil.asPlainDateTime(last.start);
				datetime = datetime.add({ days: 1 });
				start = PartialDateUtil.format({
					...PartialDateUtil.parsedFromTemporal(datetime),
					timezone: PartialDateUtil.parse(last.start).timezone,
				});
			} else {
				let date = PartialDateUtil.asPlainDate(last.start);
				date = date.add({ days: 1 });
				start = PartialDateUtil.format({
					...PartialDateUtil.parsedFromTemporal(date),
					timezone: PartialDateUtil.parse(last.start).timezone,
				});
			}
		} else if (last?.start) {
			start = last.start;
		}

		instances.push({
			venueIds: [],
			start,
		});
	}, [instances]);

	return (
		<FormSectionList
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
	const [addToAll, setAddToAll] = useState(true);

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

				if (addToAll) {
					if (!d.instances) d.instances = [];
					for (const instance of d.instances) {
						instance.venueIds.push(nextId.toString());
					}
				}
			});
			sheet.current?.dismiss();
		},
		[editor, addToAll],
	);

	const renderItem = useCallback(
		({ editor, onDelete }: { editor: Editor<Venue>; onDelete: () => void }) => (
			<EditVenueRow editor={editor} onDelete={onDelete} />
		),
		[],
	);

	const onDeleteSideEffect = useCallback(
		({ value }: { index: number; value: Venue }) => {
			editor.update((d) => {
				if (!d.instances) return;
				for (const instance of d.instances) {
					instance.venueIds = instance.venueIds.filter((id) => id !== value.id);
				}
			});
		},
		[editor],
	);

	const venues = useMemo(() => editor.field("venues", []), [editor]);

	return (
		<Fragment>
			<FormSectionList
				title="Locations"
				emptyText="No locations set"
				editor={venues}
				onAdd={() => sheet.current?.present()}
				renderItem={renderItem}
				onDelete={onDeleteSideEffect}
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
					<Button
						onPress={() => setAddToAll((s) => !s)}
						variant="subtle"
						leftSection={
							addToAll ? (
								<IconSquareCheck size={IconSize.sm} color={Colors.Primary} />
							) : (
								<IconSquare size={IconSize.sm} color={Colors.Text} />
							)
						}
					>
						Add location to all dates
					</Button>
				</Box>
			</Sheet>
		</Fragment>
	);
};
