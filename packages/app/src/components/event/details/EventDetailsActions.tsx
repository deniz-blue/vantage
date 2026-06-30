import { useRouter } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useResolvedEvent, ResolvedEventUtils } from "@vantage/core";
import { Box } from "../../base/Box";
import { IconPencil, IconShare } from "@tabler/icons-react-native";
import { IconSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { Button } from "../../base/Button";

export const EventDetailsActions = () => {
	const resolved = useResolvedEvent();
	const router = useRouter();

	const { source, id } = resolved;
	const showEdit = source.type === "local" && !!id;

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
					onPress={() => router.push(`/event/${id}/edit`)}
					children="Edit"
				/>
			)}
		</Box>
	);
};
