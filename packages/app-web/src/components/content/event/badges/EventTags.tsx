import { ActionIcon, Badge, Group, OverflowList, Popover, Stack } from "@mantine/core";
import { useQuery } from "@tanstack/react-query";
import { EventTagManager, useResolvedEvent } from "@vantage/core";
import { TagBadge } from "./TagBadge";
import { IconPencil, IconTags } from "@tabler/icons-react";
import { modals } from "@mantine/modals";

export const EventTags = ({
	withIcon,
	withEditButton,
	hideEmpty,
}: {
	withIcon?: boolean;
	withEditButton?: boolean;
	hideEmpty?: boolean;
}) => {
	const { id } = useResolvedEvent();

	const tags = useQuery({
		queryKey: ["event", id, "tags"],
		enabled: !!id,
		queryFn: async () => {
			return await EventTagManager.getTagsForEvent(id!);
		},
	});

	if (!tags.data?.length && hideEmpty) return null;

	return (
		<Group gap={4} wrap="nowrap" style={{ overflow: "auto" }}>
			{withIcon && <IconTags size={14} />}

			<OverflowList
				gap={4}
				data={tags.data ?? []}
				maxRows={1}
				flex="1"
				renderItem={(tag) => <TagBadge key={tag.id} tag={tag} />}
				renderOverflow={(items) => (
					<Popover>
						<Popover.Target>
							<Badge
								color="gray"
								variant="light"
								tt="unset"
							>
								+{items.length}
							</Badge>
						</Popover.Target>
						<Popover.Dropdown>
							<Stack gap={4}>
								{items.map(tag => (
									<TagBadge key={tag.id} tag={tag} />
								))}
							</Stack>
						</Popover.Dropdown>
					</Popover>
				)}
			/>

			{withEditButton && !!id && (
				<ActionIcon
					size="sm"
					color="gray"
					onClick={() => modals.openContextModal({
						modal: "EditEventTagsModal",
						innerProps: { eventId: id },
					})}
				>
					<IconPencil size={14} />
				</ActionIcon>
			)}
		</Group>
	)
};
