import type { EventActivity, EventInstance } from "@evnt/schema";
import { Button, Modal, Paper, Stack } from "@mantine/core";
import { Deatom, type EditAtom } from "../../edit-atom";
import { focusAtom } from "jotai-optics";
import { IconPencil } from "@tabler/icons-react";
import { useMemo } from "react";
import { atom, useAtomValue, useSetAtom } from "jotai";
import { TranslationsInput } from "../../../base/input/TranslationsInput";
import { useDisclosure } from "@mantine/hooks";
import { Trans } from "../../../content/Trans";
import { ActivitySlotInput } from "../../../base/input/ActivitySlotInput";

export const EditEventActivity = ({
	instance,
	activity,
	index,
}: {
	instance: EditAtom<EventInstance>;
	activity: EditAtom<EventActivity>;
	index: number;
}) => {
	const [opened, { close, open }] = useDisclosure();

	const onDelete = useSetAtom(useMemo(() => atom(null, (get, set) => {
		console.log("Deleting activity with index", index);
		set(instance, prev => ({
			...prev,
			activities: prev.activities?.map((activity, i) => i === index ? null : activity).filter((x): x is EventActivity => !!x) ?? [],
		}));
	}), [instance, index]));

	const nameAtom = useMemo(() => focusAtom(activity, o => o.prop("name")), [activity]);
	const slotAtom = useMemo(() => focusAtom(activity, o => o.prop("slot")), [activity]);

	const name = useAtomValue(nameAtom);

	return (
		<Paper>
			<Button
				onClick={open}
				fullWidth
				color="gray"
				justify="start"
				leftSection={<IconPencil size={16} />}
			>
				<Trans t={name} />
			</Button>

			<Modal
				opened={opened}
				onClose={close}
				size="lg"
			>
				<Stack>
					<Deatom
						atom={nameAtom}
						component={TranslationsInput}
						label="Activity Name"
					/>

					<Deatom
						atom={slotAtom}
						component={ActivitySlotInput}
						label="Slot"
					/>
				</Stack>
			</Modal>
		</Paper>
	);
};
