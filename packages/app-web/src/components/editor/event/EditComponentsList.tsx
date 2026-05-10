import { atom, useAtomValue, useSetAtom } from "jotai";
import { useMemo, useState } from "react";
import type { EditAtom } from "../edit-atom";
import type { EventComponent, EventData } from "@evnt/schema";
import { Button, Group, Input, Menu, Modal, Paper, Stack, Text, Title } from "@mantine/core";
import { IconChevronDown } from "@tabler/icons-react";
import { EditComponent } from "./EditComponent";
import { focusAtom } from "jotai-optics";
import { EventComponentRegistry } from "./event-components";
import { Trans } from "../../content/Trans";

export const EditComponentsList = ({ data }: { data: EditAtom<EventData> }) => {
	const [modalOpened, setModalOpened] = useState(false);

	const indexes = useAtomValue(useMemo(() => atom((get) => {
		return (get(data).components ?? []).map((_, i) => i);
	}), [data]));

	const addComponent = useSetAtom(useMemo(() => atom(null, (get, set, component: EventComponent) => {
		set(data, prev => ({
			...prev,
			components: [...(prev.components ?? []), component],
		}));
	}), [data]));

	const children = useMemo(() => {
		return indexes.map((i) => (
			<EditComponent
				key={i}
				index={i}
				data={data}
				component={focusAtom(data, o => o.prop("components").valueOr([]).at(i)) as EditAtom<EventComponent>}
			/>
		));
	}, [indexes, data]);

	return (
		<Stack gap={4}>
			<Group gap={4} justify="space-between">
				<Title order={4}>
					Components ({indexes.length})
				</Title>

				<Modal
					opened={modalOpened}
					onClose={() => setModalOpened(false)}
					title="Add component with type..."
					centered
				>
					<Stack>
						{Object.entries(EventComponentRegistry).map(([type, entry]) => {
							const { icon: Icon, label, desc, createData } = entry;
							return (
								<Button
									key={type}
									leftSection={<Icon />}
									onClick={() => {
										if (createData) addComponent(createData);
										setModalOpened(false);
									}}
									h="auto"
									justify="start"
									size="md"
								>
									<Stack gap={4} align="start" p={4}>
										<Text inherit span><Trans t={label} /></Text>
										{desc && <Text c="dimmed" fz="xs" fw="normal" inherit span><Trans t={desc} /></Text>}
									</Stack>
								</Button>
							);
						})}
					</Stack>
				</Modal>

				<Button
					onClick={() => setModalOpened(true)}
				>
					Add Component
				</Button>
			</Group>
			{indexes.length === 0 && (
				<Paper bg="dark" p="md" py="xl" ta="center">
					<Stack h="100%" align="center" justify="center">
						<Text c="dimmed">
							No components added yet!
						</Text>
						<Text c="dimmed" fz="xs">
							Components define additional content or metadata of the event, such as links, images, or custom sections.
						</Text>
					</Stack>
				</Paper>
			)}
			{children}
		</Stack >
	);
};
