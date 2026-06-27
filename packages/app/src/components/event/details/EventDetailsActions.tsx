import { useRouter } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useResolvedEvent, ResolvedEventUtils } from "@vantage/core";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TouchableOpacity } from "react-native";
import { IconPencil, IconShare, IconReload } from "@tabler/icons-react-native";
import { FontSize, IconSize } from "../../../theme/sizing";
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
			{isNetwork && (
				<ActionButton icon={<IconReload size={IconSize.xs} />} label="Refetch" disabled />
			)}

			{shareLink && (
				<ActionButton
					icon={<IconShare size={IconSize.xs} />}
					label="Share"
					onPress={() => Clipboard.setStringAsync(shareLink)}
				/>
			)}

			{isLocal && (
				<ActionButton
					icon={<IconPencil size={IconSize.xs} />}
					label="Edit"
					onPress={() => router.push(`/event/${id}/edit`)}
				/>
			)}
		</Box>
	);
};

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
	<Box
		component={TouchableOpacity}
		direction="row"
		gap={4}
		align="center"
		bg="BackgroundLight"
		radius={Spacing.Radius}
		px="sm"
		py={6}
		op={disabled ? 0.5 : 1}
		onPress={onPress}
		disabled={disabled || !onPress}
	>
		{icon}
		<Text fz={FontSize.sm}>{label}</Text>
	</Box>
);
