import type { EventData } from "@evnt/schema";
import { useMutation } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { atom, useSetAtom } from "jotai";
import { useMemo } from "react";
import { FormPageTemplate } from "../form";
import { Avatar, Box, Button, Modal, Stack, Text } from "@mantine/core";
import { IconDatabase } from "@tabler/icons-react";
import { getAvatarOfDid, useATProtoAuthStore } from "../../lib/atproto/useATProtoStore";
import type { AtprotoDid } from "@atcute/lexicons/syntax";
import { useAtProtoHandleQuery } from "../../lib/atproto/useAtProtoHandleQuery";
import { useDisclosure } from "@mantine/hooks";
import { dbShortcuts } from "../../db/db-shortcuts";
import { now } from "@atcute/tid";

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

	type Payload = { where: "local" | "at"; data: EventData };
	const mutation = useMutation({
		mutationFn: async ({ where, data }: Payload) => {
			if (where === "local") return await dbShortcuts.insertLocalEvent(JSON.stringify(data), { type: "directory.evnt.event" });
			if (where === "at") {
				const { rpc, agent } = useATProtoAuthStore.getState();
				if (!rpc || !agent) throw new Error("Not authenticated with ATProto");
				const res = await rpc.post("com.atproto.repo.putRecord", {
					input: {
						collection: "directory.evnt.event",
						record: {
							...data,
							"$type": "directory.evnt.event",
						},
						repo: agent.sub,
						rkey: now(),
					},
				});
				if (!res.ok) throw new Error(res.data.error + ": " + res.data.message);
				return await dbShortcuts.insertEventMeta({
					source: { type: "at", uri: res.data.uri },
					format: { type: "directory.evnt.event" },
				});
			};
			return null;
		},
		onSuccess: async (id) => {
			if (!id) return;
			navigate({ to: "/event", search: { id } });
		},
	});

	const create = useSetAtom(useMemo(() => atom(null, async (get, set, where: "local" | "at") => {
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
