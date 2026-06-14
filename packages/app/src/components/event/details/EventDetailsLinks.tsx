import { Linking, TouchableOpacity } from "react-native";
import { useResolvedEvent } from "@vantage/core";
import type { LinkComponent } from "@evnt/types";
import { IconExternalLink } from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { Sizing } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { SmallTitle } from "./SmallTitle";

export const EventDetailsLinks = () => {
	const { data } = useResolvedEvent();

	const links = data?.components?.filter(
		(c): c is LinkComponent => c.$type === "directory.evnt.component.link",
	);

	if (!links || links.length === 0) return null;

	return (
		<Box gap={Spacing.xs}>
			<SmallTitle>Links</SmallTitle>
			{links.map((link, i) => (
				<Box
					key={i}
					component={TouchableOpacity}
					direction="row"
					gap={6}
					align="center"
					py={4}
					onPress={() => Linking.openURL(link.url)}
					disabled={link.disabled}
				>
					{link.name
						? <TransText fz={Sizing.fontSizeMd} value={link.name} />
						: <Text fz={Sizing.fontSizeMd} numberOfLines={1}>{link.url}</Text>
					}
					<IconExternalLink size={14} color="TextDimmed" />
				</Box>
			))}
		</Box>
	);
};
