import { useRouter } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useResolvedEvent, ResolvedEventUtils } from "@vantage/core";
import { Box } from "../../base/Box";
import { IconDotsVertical, IconPencil, IconShare } from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { IconSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { Button } from "../../base/button/Button";
import { useState } from "react";
import { EventActionsSheet } from "../../app/EventActionsSheet";

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
					leftSection={<IconShare size={IconSize.xs} color={Colors.Text} />}
					onPress={() => Clipboard.setStringAsync(shareLink)}
					children="Share"
				/>
			)}

			{showEdit && (
				<Button
					size="sm"
					leftSection={<IconPencil size={IconSize.xs} color={Colors.Text} />}
					onPress={() => router.push(`/event/${resolved.id}/edit`)}
					children="Edit"
				/>
			)}

			<Button
				size="sm"
				leftSection={<IconDotsVertical size={IconSize.xs} color={Colors.Text} />}
				children="More"
				onPress={() => setOpen(!open)}
			/>

			<EventActionsSheet open={open} onClose={() => setOpen(false)} />
		</Box>
	);
};
