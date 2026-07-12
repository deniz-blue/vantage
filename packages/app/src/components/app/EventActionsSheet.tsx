import { useRouter } from "expo-router";
import { Sheet } from "../base/Sheet";
import { EventsManager, ResolvedEventUtils, useResolvedEvent } from "@vantage/core";
import { createActionsForEvent } from "../actions/event-actions";
import { IconPencil, IconShare, IconX } from "@tabler/icons-react-native";
import { IconSize } from "../../theme/sizing";
import { ActionButtonList } from "../actions/ActionButton";
import { Box } from "../base/Box";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button } from "../base/button/Button";
import { Text } from "../base/Text";

export const EventActionsSheet = ({ open, onClose }: { open: boolean; onClose: () => void }) => {
	const router = useRouter();
	const resolved = useResolvedEvent();
	const actions = [...createActionsForEvent(resolved)];
	const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);

	const deleteMutation = useMutation({
		mutationFn: async () => {
			if (!resolved.id) throw new Error("No event ID");
			await EventsManager.removeEvent(resolved.id);
		},
		onSuccess: () => {
			onClose();
			setShowDeleteConfirmation(false);
			if (router.canGoBack()) router.back();
			else router.replace("/");
		},
	});

	const shareLink = ResolvedEventUtils.createShareLink(resolved);

	if (shareLink)
		actions.push({
			label: "Share",
			type: "copy",
			value: shareLink,
			icon: <IconShare size={IconSize.xs} />,
		});

	if (resolved.source.type === "local")
		actions.push({
			label: "Edit",
			type: "fn",
			onRun: () => router.push(`/event/${resolved.id}/edit`),
			icon: <IconPencil size={IconSize.xs} />,
		});

	if (resolved.id)
		actions.push({
			label: "Delete",
			type: "fn",
			onRun: () => setShowDeleteConfirmation(true),
			icon: <IconX size={IconSize.xs} />,
			danger: true,
		});

	return (
		<Sheet open={open} onClose={onClose}>
			<Box p="sm" gap="sm">
				<ActionButtonList actions={actions} />
			</Box>

			<Sheet open={showDeleteConfirmation} onClose={() => setShowDeleteConfirmation(false)}>
				<Box p="sm" gap="sm">
					<Text>
						{resolved.source.type !== "local"
							? "Are you sure you want to stop following this event?"
							: "Are you sure you want to delete this event? This action cannot be undone."}
					</Text>
					<Button
						size="sm"
						variant="danger"
						children="Delete"
						onPress={() => deleteMutation.mutate()}
						loading={deleteMutation.isPending}
					/>
				</Box>
			</Sheet>
		</Sheet>
	);
};
