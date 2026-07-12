import { Linking } from "react-native";
import { useResolvedEvent } from "@vantage/core";
import type { LinkComponent } from "@evnt/types";
import { IconExternalLink } from "@tabler/icons-react-native";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { TransText } from "../../core/TransText";
import { FontSize, IconSize } from "../../../theme/sizing";
import { Spacing } from "../../../theme/spacing";
import { SmallTitle } from "./SmallTitle";
import { Button } from "../../base/button/Button";

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
				<Button
					key={i}
					onPress={() => Linking.openURL(link.url)}
					disabled={link.disabled}
					leftSection={<IconExternalLink size={IconSize.xs} />}
				>
					{link.name ? (
						<TransText fz={FontSize.sm} value={link.name} />
					) : (
						<Text fz={FontSize.sm} numberOfLines={1}>
							{link.url}
						</Text>
					)}
				</Button>
			))}
		</Box>
	);
};
