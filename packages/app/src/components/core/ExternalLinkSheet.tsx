import { Linking } from "react-native";
import { Sheet } from "../base/Sheet";
import { Box } from "../base/Box";
import { InputWrapper } from "../base/input/InputWrapper";
import { Text } from "../base/Text";
import { Colors } from "../../theme/colors";
import { FontSize } from "../../theme/sizing";
import { Button } from "../base/button/Button";
import { AppCopyButton } from "./AppCopyButton";

export const ExternalLinkSheet = ({
	link,
	onClose,
}: {
	link: string | null;
	onClose: () => void;
}) => {
	const openLink = () => Linking.openURL(link ?? "");

	return (
		<Sheet open={!!link} onClose={onClose}>
			<Box p="sm" gap="sm">
				<Box>
					<InputWrapper label="External Link" />
					<Text c={Colors.Blue} fz={FontSize.sm} onPress={openLink}>
						{link}
					</Text>
				</Box>
				<Button onPress={openLink} children="Open Link" />
				<AppCopyButton value={link ?? ""} children="Copy Link" />
			</Box>
		</Sheet>
	);
};
