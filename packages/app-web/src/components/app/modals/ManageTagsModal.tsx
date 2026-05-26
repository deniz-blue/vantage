import { ActionIcon, Badge, Button, Group, Paper, Stack, Text } from "@mantine/core";
import { type ContextModalProps } from "@mantine/modals";
import { notifications } from "@mantine/notifications";
import { useMutation, useQuery } from "@tanstack/react-query";
import { queryClient, TagsManager } from "@vantage/core";
import { modals } from "@mantine/modals";
import { withConfirmation } from "../../../lib/util/confirm";
import { schema } from "@vantage/db";
import { IconPencil, IconTrash } from "@tabler/icons-react";

export const ManageTagsModal = ({
	context,
	id,
}: ContextModalProps<Record<string, never>>) => {
	const tags = useQuery({
		queryKey: ["tags", "all"],
		queryFn: async () => await TagsManager.getAll(),
	});

	const deleteTag = useMutation({
		mutationFn: async (tagId: schema.Tag["id"]) => {
			await TagsManager.delete(tagId);
		},
		onSuccess: async () => {
			await queryClient.invalidateQueries({ queryKey: ["tags", "all"] });
			await queryClient.invalidateQueries({ predicate: query => query.queryKey[0] === "event" && query.queryKey[2] === "tags" });
			notifications.show({ title: "Deleted", message: "Tag deleted", color: "green" });
		},
		onError: (err: any) => {
			notifications.show({ title: "Error", message: String(err?.message ?? err), color: "red" });
		},
	});

	return (
		<Stack>
			<Group justify="space-between" align="center">
				<Text fw={600}>Manage Tags</Text>

				<Button
					onClick={() => modals.openContextModal({
						modal: "EditTagModal",
						innerProps: {},
					})}
				>
					Create Tag
				</Button>
			</Group>

			<Stack gap="xs">
				{tags.data?.map(tag => (
					<Paper key={tag.id} withBorder p="xs">
						<Group justify="space-between" align="center">
							<Group gap={4}>
								<ActionIcon
									variant="subtle"
									color="gray"
									onClick={() => modals.openContextModal({
										modal: "EditTagModal",
										innerProps: { tagId: tag.id },
									})}
								>
									<IconPencil />
								</ActionIcon>
								<Badge
									tt="unset"
									size="lg"
									color={tag.color ?? "gray"}
									variant="light"
								>
									{tag.name}
								</Badge>
							</Group>

							<Group gap={4}>
								<ActionIcon
									variant="subtle"
									color="red"
									onClick={withConfirmation(
										`Delete tag \"${tag.name}\"? This will remove it from any events that use it.`,
										() => deleteTag.mutate(tag.id)
									)}
									loading={deleteTag.isPending}
								>
									<IconTrash />
								</ActionIcon>
							</Group>
						</Group>
					</Paper>
				))}
			</Stack>

			<Button
				color="gray"
				onClick={() => context.closeModal(id)}
			>
				Close
			</Button>
		</Stack>
	);
};