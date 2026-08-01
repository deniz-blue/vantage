import { Linking } from "react-native";
import { Sheet, SheetRef } from "../base/sheet/Sheet";
import { Box } from "../base/Box";
import { InputWrapper } from "../base/input/InputWrapper";
import { Text } from "../base/Text";
import { Colors } from "../../theme/colors";
import { FontSize } from "../../theme/sizing";
import { Button } from "../base/button/Button";
import { AppCopyButton } from "./AppCopyButton";
import { useEffect, useRef } from "react";

export const ExternalLinkSheet = ({
	link,
	onClose,
}: {
	link: string | null;
	onClose: () => void;
}) => {
	const sheet = useRef<SheetRef>(null);

	const openLink = () => Linking.openURL(link ?? "");

	// Jank alert !!!
	useEffect(() => {
		if (link) sheet.current?.present();
		else sheet.current?.dismiss();
	}, [link]);

	return (
		<Sheet ref={sheet} onDidDismiss={onClose}>
			<Box gap="sm">
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
