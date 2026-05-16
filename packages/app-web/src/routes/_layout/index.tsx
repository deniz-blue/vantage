import { createFileRoute } from "@tanstack/react-router";
import { Container, Group, Stack, Title } from "@mantine/core";
import { IconHome } from "@tabler/icons-react";
import { useHomeStore } from "../../components/app/home/useHomeStore";
import { HomeWidget } from "../../components/app/home/HomeWidget";

export const Route = createFileRoute("/_layout/")({
	component: HomePage,
	staticData: {
		spaceless: true,
	},
})

export default function HomePage() {
	const widgets = useHomeStore(store => store.widgets);

	return (
		<Container size="lg" px="xs">
			<Stack gap={4}>
				<Group gap={4} p="xs" pt="md" justify="center">
					<IconHome />
					<Title order={3}>
						Home
					</Title>
				</Group>

				{widgets.map((widget, index) => (
					<HomeWidget key={index} widget={widget} />
				))}
			</Stack>
		</Container>
	)
}
