import { useRouter } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useResolvedEvent, ResolvedEventUtils } from "@vantage/core";
import { Box } from "../../base/Box";
import { IconDotsVertical, IconPencil, IconShare } from "@tabler/icons-react-native";
import { Colors } from "../../../theme/colors";
import { IconSize } from "../../../theme/sizing";
import { Button } from "../../base/button/Button";
import { useRef } from "react";
import { EventActionsSheet } from "../../app/EventActionsSheet";
import { SheetRef } from "../../base/sheet/Sheet";

export const EventDetailsActions = () => {
	const sheet = useRef<SheetRef>(null);
	const resolved = useResolvedEvent();
	const router = useRouter();

	const showEdit = resolved.source.type === "local" && !!resolved.id;

	const shareLink = ResolvedEventUtils.createShareLink(resolved);

	return (
		<Box direction="row" justify="flex-end" gap="xs" wrap="wrap">
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
				onPress={() => sheet.current?.present()}
			/>

			<EventActionsSheet sheet={sheet} />
		</Box>
	);
};
