import type { EventActivity, EventData, EventInstance, Venue } from "@evnt/schema";
import { Button, Group, Input, Paper, Stack, Text, Title } from "@mantine/core";
import { atom, useAtomValue, useSetAtom } from "jotai";
import { focusAtom } from "jotai-optics";
import type { EditAtom } from "../../edit-atom";
import { useMemo } from "react";
import { EditEventActivity } from "./EditEventActivity";

export const EditEventActivityList = ({ data }: { data: EditAtom<EventInstance> }) => {
	const lengthAtom = useMemo(() => atom((get) => get(data).activities?.length ?? 0), [data]);
	const length = useAtomValue(lengthAtom);
	const setData = useSetAtom(data);

	const children = new Array(length).fill(0).map((_, i) => (
		<EditEventActivity
			key={i}
			index={i}
			instance={data}
			activity={focusAtom(data, o => o.prop("activities").valueOr([]).at(i)) as EditAtom<EventActivity>}
		/>
	));

	return (
		<Stack gap={4}>
			<Group gap={4} justify="space-between">
				<Input.Label>
					Activities ({length})
				</Input.Label>
			</Group>

			<Stack gap={4}>
				{children}
			</Stack>

			<Group justify="start">
				<Button
					size="xs"
					onClick={() => {
						const newActivity: EventActivity = {
							name: { en: "New Activity" },
						};

						setData((prev) => ({
							...prev,
							activities: [...(prev.activities ?? []), newActivity],
						}));
					}}
				>
					Add Activity
				</Button>
			</Group>
		</Stack>
	);
};
