import { Image } from "expo-image";
import { Box } from "../base/Box";
import { Card } from "../base/Card";
import { Text } from "../base/Text";
import { FontSize, IconSize } from "../../theme/sizing";
import { ButtonBase } from "../base/ButtonBase";
import { useQuery } from "@tanstack/react-query";
import { didDocumentResolver } from "@vantage/atproto";
import { AtprotoDid } from "@atcute/lexicons/syntax";
import { IconCheck, IconDotsVertical } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { ActionIcon } from "../base/button/ActionIcon";
import { Loader } from "../base/Loader";

export const useDidDocument = (did: AtprotoDid) => {
	return useQuery({
		queryKey: ["atproto", "didDocument", did],
		queryFn: () => didDocumentResolver.resolve(did),
	});
};

export const useAtHandle = (did: AtprotoDid) => {
	const didDocument = useDidDocument(did);
	const handle = didDocument.data?.alsoKnownAs?.[0].slice("at://".length);
	return handle;
};

export const useAtAvatarUrl = (did: AtprotoDid) => {
	return `https://blobs.blue/${did}/avatar`;
};

export const AtUserCard = ({
	did,
	active,
	loading,
	onPress,
	onLongPress,
	onMenu,
}: {
	did: AtprotoDid;
	active?: boolean;
	loading?: boolean;
	onPress?: () => void;
	onLongPress?: () => void;
	onMenu?: () => void;
}) => {
	const avatarUrl = useAtAvatarUrl(did);
	const handle = useAtHandle(did);

	return (
		<ButtonBase onPress={onPress} onLongPress={onLongPress}>
			<Card>
				<Box direction="row" justify="space-between">
					<Box direction="row" align="center" gap="xs">
						<Box
							component={Image}
							source={avatarUrl}
							w={IconSize.xl}
							h={IconSize.xl}
							radius={999}
						/>
						<Box>
							<Text fw="bold" fz={FontSize.sm}>
								{handle ?? "Loading..."}
							</Text>
							<Text c="TextDimmed" fz={FontSize.xs}>
								{did}
							</Text>
						</Box>
					</Box>
					<Box direction="row" align="center" gap="xs">
						{loading && <Loader size="small" />}
						{active && <IconCheck size={IconSize.sm} color={Colors.Green} />}
						<ActionIcon variant="subtle" onPress={onMenu}>
							<IconDotsVertical size={IconSize.sm} color={Colors.Text} />
						</ActionIcon>
					</Box>
				</Box>
			</Card>
		</ButtonBase>
	);
};
