import { useRouter } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useResolvedEvent, ResolvedEventUtils, EventsManager } from "@vantage/core";
import { Box } from "../../base/Box";
import { IconDotsVertical, IconPencil, IconShare } from "@tabler/icons-react-native";
import { IconSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { Button } from "../../base/button/Button";
import { Fragment, useState } from "react";
import { Sheet } from "../../base/Sheet";
import { CopyButton } from "../../base/button/CopyButton";
import { useMutation } from "@tanstack/react-query";
import { Text } from "../../base/Text";
import { AppCopyButton } from "../../core/AppCopyButton";

export const EventDetailsActions = () => {
	const [open, setOpen] = useState(false);
	const resolved = useResolvedEvent();
	const router = useRouter();

	const showEdit = resolved.source.type === "local" && !!resolved.id;

	const shareLink = ResolvedEventUtils.createShareLink(resolved);

	return (
		<Box direction="row" justify="flex-end" gap={Spacing.xs} wrap="wrap">
			{shareLink && (
				<Button
					size="sm"
					leftSection={<IconShare size={IconSize.xs} />}
					onPress={() => Clipboard.setStringAsync(shareLink)}
					children="Share"
				/>
			)}

			{showEdit && (
				<Button
					size="sm"
					leftSection={<IconPencil size={IconSize.xs} />}
					onPress={() => router.push(`/event/${resolved.id}/edit`)}
					children="Edit"
				/>
			)}

			<Button
				size="sm"
				leftSection={<IconDotsVertical size={IconSize.xs} />}
				children="More"
				onPress={() => setOpen(!open)}
			/>

			<Sheet open={open} onClose={() => setOpen(false)}>
				<EventActionsMenu onClose={() => setOpen(false)} />
			</Sheet>
		</Box>
	);
};

export const EventActionsMenu = ({
	onClose,
}: {
	onClose: () => void;
}) => {
	const resolved = useResolvedEvent();

	return (
		<Box p="sm" gap="sm">
			{resolved.data && (
				<AppCopyButton size="sm" value={JSON.stringify(resolved.data)}>
					Copy JSON
				</AppCopyButton>
			)}

			<EventDeleteButton />
		</Box>
	);
};

export const EventDeleteButton = () => {
	const resolved = useResolvedEvent();
	const router = useRouter();
	const [confirm, setConfirm] = useState(false);

	const mut = useMutation({
		mutationFn: async () => {
			if (!resolved.id) throw new Error("No event ID");
			await EventsManager.removeEvent(resolved.id);
		},
		onSuccess: () => {
			if (router.canGoBack()) router.back();
			else router.replace("/");
		},
	});

	if (!resolved.id) return null;

	return (
		<Fragment>
			<Button
				size="sm"
				variant="danger"
				children="Delete Event"
				onPress={() => setConfirm(true)}
			/>

			<Sheet open={confirm} onClose={() => setConfirm(false)}>
				<Box p="sm" gap="sm">
					<Text>
						{resolved.source.type !== "local" ? "Are you sure you want to stop following this event?" : "Are you sure you want to delete this event? This action cannot be undone."}
					</Text>
					<Button
						size="sm"
						variant="danger"
						children="Delete"
						onPress={mut.mutate}
						loading={mut.isPending}
					/>
				</Box>
			</Sheet>
		</Fragment>
	);
};
