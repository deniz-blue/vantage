import type { EventData } from "@evnt/schema";
import { useMutation } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { atom, useSetAtom } from "jotai";
import { useMemo } from "react";
import { EventActions } from "../../lib/actions/event-actions";
import { FormPageTemplate } from "../form";
import { Avatar, Box, Button, Group, Menu, Modal, Stack, Text } from "@mantine/core";
import { IconChevronDown, IconDatabase, IconWorldOff } from "@tabler/icons-react";
import type { EventSource } from "../../db/models/event-source";
import { getAvatarOfDid, useATProtoAuthStore } from "../../lib/atproto/useATProtoStore";
import type { AtprotoDid } from "@atcute/lexicons/syntax";
import { useAtProtoHandleQuery } from "../../lib/atproto/useAtProtoHandleQuery";
import { useDisclosure } from "@mantine/hooks";

export const Route = createFileRoute("/_layout/new")({
	component: NewPage,
	staticData: {
		hasEventForm: true,
	},
})

function NewPage() {
	const [modalOpened, { open, close }] = useDisclosure(false);

	const dataAtom = useMemo(() => atom<EventData | null>({ v: "0.1", name: {} }), []);
	const agent = useATProtoAuthStore(store => store.agent);
	const handle = useAtProtoHandleQuery(agent?.sub as AtprotoDid);
	const navigate = useNavigate();

	type Payload = { where: EventSource.Type; data: EventData };
	const mutation = useMutation({
		mutationFn: async ({ where, data }: Payload) => {
			if (where === "local") return await EventActions.createLocalEvent(data);
			if (where === "at") return await EventActions.createATProtoEvent(data);
			return null;
		},
		onSuccess: async (source) => {
			if (!source) return;
			navigate({ to: "/event", search: { source } });
		},
	});

	const create = useSetAtom(useMemo(() => atom(null, async (get, set, where: EventSource.Type) => {
		const data = get(dataAtom);
		if (!data) return;
		mutation.mutate({ where, data });
	}), [dataAtom, mutation]));

	return (
		<FormPageTemplate
			title="New Event"
			loading={mutation.isPending}
			data={dataAtom}
			button={(
				<Box>
					<Button
						color="green"
						loading={mutation.isPending}
						onClick={open}
					>
						Create
					</Button>
					<Modal
						opened={modalOpened}
						onClose={close}
						centered
						title="Create event on..."
					>
						<Stack>
							<Button
								onClick={() => create("local")}
								leftSection={<IconDatabase />}
								justify="start"
							>
								This device
							</Button>
							<Button
								onClick={() => create("at")}
								disabled={!agent}
								justify="start"
								leftSection={(
									<Avatar
										src={agent ? getAvatarOfDid(agent?.sub) : null}
										size={24}
									/>
								)}
							>
								<Text inherit ff="monospace">
									{handle.data ? `@${handle.data}` : (agent?.sub ? agent.sub : "AT Protocol")}
								</Text>
							</Button>
						</Stack>
					</Modal>
				</Box>
			)}
		/>
	)
}
