import { Box, Button, Chip, Collapse, Group, Input, Stack } from "@mantine/core";
import { TranslationsInput } from "../../base/input/TranslationsInput";
import { EventStatusPicker } from "./EventStatusPicker";
import { atom, useAtomValue, type WritableAtom } from "jotai";
import type { EventData } from "@evnt/schema";
import { focusAtom } from "jotai-optics";
import { Deatom } from "../edit-atom";
import { useMemo, useState } from "react";
import { TranslationsUtil } from "@evnt/translations";

export const EditEventDetails = ({ data }: { data: WritableAtom<EventData, [EventData], void> }) => {
	const nameAtom = useMemo(() => focusAtom(data, (o) => o.prop("name")), [data]);
	const labelAtom = useMemo(() => focusAtom(data, (o) => o.prop("label").valueOr({})), [data]);
	const statusAtom = useMemo(() => focusAtom(data, (o) => o.prop("status").valueOr("planned")), [data]);

	const hasLabel = useAtomValue(useMemo(() => atom((get) => {
		const event = get(data);
		return !TranslationsUtil.isEmpty(event.label);
	}), [data]));

	const [showLabel, setShowLabel] = useState(TranslationsUtil.isEmpty() ? false : true);

	return (
		<Stack>
			<Stack gap={4}>
				<Group justify="space-between" gap={4}>
					<Stack gap={4}>
						<Input.Label>Event Name</Input.Label>
						<Input.Description>
							Shows up as the main title of the event
						</Input.Description>
					</Stack>
					{!hasLabel && (
						<Chip
							size="xs"
							color="gray"
							checked={showLabel}
							onChange={(checked) => setShowLabel(checked)}
						>
							Label
						</Chip>
					)}
				</Group>
				<Deatom
					component={TranslationsInput}
					atom={nameAtom}
					placeholder="My Cool Event"
					required
				/>
			</Stack>

			<Collapse expanded={showLabel}>
				<Deatom
					component={TranslationsInput}
					atom={labelAtom}
					label="Event Label"
					description="A short, descriptive label. Shows up under the event name."
					placeholder="Friend Group Hangout"
				/>
			</Collapse>

			<Group grow>
				<Stack gap={0}>
					<Input.Label>Event Status</Input.Label>
					<Input.Description>
						The event's progress and availability
					</Input.Description>
				</Stack>
				<Deatom
					component={EventStatusPicker}
					atom={statusAtom}
				/>
			</Group>
		</Stack>
	);
};

