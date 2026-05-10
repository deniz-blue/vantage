import { Badge, Button, Group, Stack } from "@mantine/core";
import { IconArrowNarrowRight } from "@tabler/icons-react";
import { Link } from "@tanstack/react-router";
import { useResolvedEvent } from "../../../../db/resolved-event";

export const EventCardBottom = () => {
	const { source } = useResolvedEvent();

	return (
		<Stack>
			<Group gap={0} justify="space-between">
				<Group>
					{source.type === "local" && (
						<Badge color="gray" size="xs" variant="outline" children="local" />
					)}
					{source.type === "at" && (
						<Badge color="blue" size="xs" variant="outline" children="atproto" />
					)}
				</Group>
				<Group>
					<Button
						size="compact-sm"
						variant="light"
						rightSection={<IconArrowNarrowRight />}
						color="gray"
						renderRoot={(props) => (
							<Link
								to="/event"
								search={{ source }}
								{...props}
							/>
						)}
					>
						View
					</Button>
				</Group>
			</Group>
		</Stack>
	);
};
