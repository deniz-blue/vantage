import { useRouter } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useResolvedEvent, ResolvedEventUtils } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TouchableOpacity } from "react-native";
import { IconPencil, IconShare, IconReload } from "@tabler/icons-react-native";
import { Sizing } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";

export const EventDetailsActions = () => {
	const resolved = useResolvedEvent();
	const router = useRouter();

	const { source, id } = resolved;
	const isLocal = source.type === "local" && !!id;
	const isNetwork = ResolvedEventUtils.isNetworkSource(resolved) && !!id;

	const shareLink = ResolvedEventUtils.createShareLink(resolved);

	return (
		<Box direction="row" gap={Spacing.xs} wrap="wrap">
			{/* Refetch (network sources only — stubbed) */}
			{/* TODO: implement refetch when AT protocol auth is available */}
			{isNetwork && (
				<ActionButton icon={<IconReload size={16} />} label="Refetch" disabled />
			)}

			{/* Share */}
			{shareLink && (
				<ActionButton
					icon={<IconShare size={16} />}
					label="Share"
					onPress={() => {
						Clipboard.setStringAsync(shareLink);
					}}
				/>
			)}

			{/* Edit (local sources only) */}
			{isLocal && (
				<ActionButton
					icon={<IconPencil size={16} />}
					label="Edit"
					onPress={() => router.push(`/event/${id}/edit`)}
				/>
			)}
		</Box>
	);
};

// === Action button ===

const ActionButton = ({
	icon,
	label,
	onPress,
	disabled,
}: {
	icon: React.ReactNode;
	label: string;
	onPress?: () => void;
	disabled?: boolean;
}) => (
	<TouchableOpacity onPress={onPress} disabled={disabled || !onPress}>
		<Box
			direction="row"
			gap={4}
			align="center"
			bg="BackgroundLight"
			radius={Spacing.Radius}
			px="sm"
			py={6}
			op={disabled ? 0.5 : 1}
		>
			{icon}
			<Text fz={Sizing.fontSizeSm}>{label}</Text>
		</Box>
	</TouchableOpacity>
);
