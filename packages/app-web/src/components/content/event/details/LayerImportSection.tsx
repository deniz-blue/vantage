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

	const existing = useQuery({
		queryKey: ["exists", { source, format }],
		enabled: !id && !unknown,
		queryFn: async () => {
			return await dbShortcuts.getFromMeta(source, format);
		},
	});

	return (
		<Stack gap={0}>
			<Collapse expanded={!id && !unknown && (existing.data?.length ?? 0) > 0}>
				<Button
					fullWidth
					onClick={() => {
						const existingId = existing.data?.[0].id;
						if (existingId) {
							navigate({
								to: "/event",
								search: { id: existingId },
								replace: true,
							});
						}
					}}
					color="blue"
					h="auto"
				>
					<Stack gap={4} py={4}>
						<Text span inherit>
							View in My Events
						</Text>
						<Text size="xs" fw="normal" span inherit>
							This event is already in your list
						</Text>
					</Stack>
				</Button>
			</Collapse>
			<Collapse expanded={!id && !unknown && (existing.data?.length ?? 0) === 0}>
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
						replace: true,
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
