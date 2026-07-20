import { Box } from "../base/Box";
import { Text } from "../base/Text";
import { AppCopyButton } from "../core/AppCopyButton";
import { FontSize } from "../../theme/sizing";
import { ScrollView } from "react-native-gesture-handler";

export const ViewRawSheetContent = ({ raw }: { raw: string }) => {
	let json = null;
	try {
		json = JSON.parse(raw);
	} catch {}

	const str = json ? JSON.stringify(json, null, 2) : raw;

	return (
		<ScrollView>
			<Box gap="md" p="md" w="100%">
				<Box direction="row" gap="sm" w="100%">
					<AppCopyButton value={str} w="100%">
						Copy
					</AppCopyButton>
				</Box>
				<Text selectable fz={FontSize.xs} style={{ fontFamily: "monospace" }}>
					{str}
				</Text>
			</Box>
		</ScrollView>
	);
};
