import { Button, Collapse, Stack, Text } from "@mantine/core";
import { AsyncAction } from "../../../data/AsyncAction";
import { useResolvedEvent } from "@vantage/core";
import { useQuery } from "@tanstack/react-query";
import { dbShortcuts } from "../../../../db/db-shortcuts";
import { notifications } from "@mantine/notifications";
import { useNavigate } from "@tanstack/react-router";

export const LayerImportSection = () => {
	const { id, source, format } = useResolvedEvent();
	const navigate = useNavigate();

	const unknown = source.type == "unknown" || format.type == "unknown";

	const existsQuery = useQuery({
		queryKey: ["exists", { source, format }],
		enabled: !id && !unknown,
		queryFn: async () => {
			return await dbShortcuts.eventMetaExists(source, format);
		},
	});

	return (
		<Stack>
			<Collapse expanded={!id && !unknown && existsQuery.data === false}>
				<AsyncAction action={async () => {
					const id = await dbShortcuts.insertEventMeta({ source, format });
					notifications.show({
						title: "Event Added",
						message: "The event has been added to your list and is now available offline.",
						color: "green",
					});
					navigate({
						to: "/event",
						search: { id },
					});
				}}>
					{({ loading, onClick }) => (
						<Button
							fullWidth
							onClick={onClick}
							loading={loading}
							color="green"
							h="auto"
						>
							<Stack gap={4} py={4}>
								<Text span inherit>
									Add to My Events
								</Text>
								<Text size="xs" fw="normal" span inherit>
									offline accessible and shown in your list
								</Text>
							</Stack>
						</Button>
					)}
				</AsyncAction>
			</Collapse>
		</Stack>
	);
};
