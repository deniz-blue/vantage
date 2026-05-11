import { Group, Stack, Text } from "@mantine/core";
import { snippetEvent } from "@evnt/pretty";
import { Snippet } from "../Snippet";
import { useResolvedEvent } from "@vantage/core";

export const EventInstanceList = () => {
	const { data } = useResolvedEvent();

	if (!data) return null;

	const snippets = snippetEvent(data, {
		maxVenues: 3,
		maxInstances: 5,
		maxGroups: 5,
	});

	return (
		<Stack gap={4}>
			{snippets.map((snippet, index) => (
				<Snippet key={index} snippet={snippet} />
			))}
		</Stack>
	);
}
