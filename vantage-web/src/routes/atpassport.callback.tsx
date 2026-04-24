import { notifications } from "@mantine/notifications";
import { createFileRoute, useNavigate } from "@tanstack/react-router"
import { useEffect } from "react";
import { useATProtoAuthStore } from "../lib/atproto/useATProtoStore";
import { Box, Stack, Text } from "@mantine/core";
import { CenteredLoader } from "../components/content/base/CenteredLoader";

export const Route = createFileRoute("/atpassport/callback")({
	component: RouteComponent,
})

function RouteComponent() {
	const navigate = useNavigate();

	useEffect(() => {
		const params = new URLSearchParams(window.location.search);
		console.log("OAuth callback received with params:", params.toString());

		const keys = ["handle", "did"];
		if (keys.some(key => !params.has(key))) {
			console.error("Missing required parameters. Received params:", params.toString());
			navigate({
				to: "/",
				replace: true,
			});
			notifications.show({
				title: "Error",
				message: "Missing atpassport callback parameters",
				color: "red",
			})
			return
		}

		// because ummm uhmmm
		setTimeout(async () => {
			await useATProtoAuthStore.getState().startAuthorization(params.get("did")!);
		}, 500);
	}, []);

	return (
		<Box h="90vh" pt="xl">
			<CenteredLoader>
				<Stack align="center" gap="xs">
					<Text size="lg">Signing you in...</Text>
					<Text c="dimmed">You will be redirected shortly...</Text>
				</Stack>
			</CenteredLoader>
		</Box>
	)
}
