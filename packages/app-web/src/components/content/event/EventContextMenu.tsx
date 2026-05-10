import { ActionIcon, Menu } from "@mantine/core";
import { IconDotsVertical } from "@tabler/icons-react";
import { useNavigate } from "@tanstack/react-router";
import { EventActionFactory } from "../../../hooks/actions/useProvideEventActions";
import type { Action } from "../../app/overlay/spotlight/useActionsStore";
import { useResolvedEvent } from "../../../db/resolved-event";

export const EventContextMenu = () => {
	const resolved = useResolvedEvent();
	const navigate = useNavigate();

	const actions: Action[] = EventActionFactory.All({
		resolved,
		navigate,
	});

	return (
		<Menu>
			<Menu.Target>
				<ActionIcon
					variant="subtle"
					color="gray"
					size="sm"
				>
					<IconDotsVertical />
				</ActionIcon>
			</Menu.Target>
			<Menu.Dropdown>
				{actions.filter(x => !x.disabled).map((action, i) => (
					<Menu.Item
						key={i}
						leftSection={action.icon}
						onClick={action.execute}
						color={action.special?.color}
					>
						{action.label}
					</Menu.Item>
				))}
			</Menu.Dropdown>
		</Menu>
	);
};
