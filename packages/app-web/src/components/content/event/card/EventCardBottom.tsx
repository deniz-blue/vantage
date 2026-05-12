import { Badge, Button, Group, Stack, Tooltip } from "@mantine/core";
import { IconArrowNarrowRight } from "@tabler/icons-react";
import { Link } from "@tanstack/react-router";
import { useResolvedEvent } from "@vantage/core";

export const EventCardBottom = () => {
	const { id, source, format } = useResolvedEvent();

	return (
		<Stack>
			<Group gap={0} justify="space-between">
				<Group gap={4}>
					{source.type === "local" && (
						<Tooltip label="Saved on Browser/Device">
							<Badge color="gray" size="xs" variant="outline" children="L" />
						</Tooltip>
					)}
					{source.type === "http" && (
						<Tooltip label="HTTP">
							<Badge color="green" size="xs" variant="outline" children="H" />
						</Tooltip>
					)}
					{source.type === "at" && (
						<Tooltip label="AT Protocol">
							<Badge color="blue" size="xs" variant="outline" children="@" />
						</Tooltip>
					)}
					{source.type === "mediawiki" && (
						<Tooltip label="MediaWiki">
							<Badge color="purple" size="xs" variant="outline" children="W" />
						</Tooltip>
					)}
					{format.type === "ics" && (
						<Tooltip label="Apple iCalendar / ICS Format">
							<Badge color="orange" size="xs" variant="outline" children="ICS" />
						</Tooltip>
					)}
					{format.type === "community.lexicon.calendar.event" && (
						<Tooltip label="Lexicon Community Event Format">
							<Badge color="cyan" size="xs" variant="outline" children="C" />
						</Tooltip>
					)}
					{format.type === "unknown" && (
						<Tooltip label="Unknown Format">
							<Badge color="yellow" size="xs" variant="outline" children="?" />
						</Tooltip>
					)}
				</Group>
				<Group>
					{id && (
						<Button
							size="compact-sm"
							variant="light"
							rightSection={<IconArrowNarrowRight />}
							color="gray"
							renderRoot={(props) => (
								<Link
									to="/event"
									search={{ id }}
									{...props}
								/>
							)}
						>
							View
						</Button>
					)}
				</Group>
			</Group>
		</Stack>
	);
};
