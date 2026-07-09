import { useState } from "react";
import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { Text } from "../base/Text";
import { AppCopyButton } from "../core/AppCopyButton";
import { ScrollView } from "react-native";

export const ViewRawSheetContent = ({ raw }: { raw: string }) => {
	const [wrap, setWrap] = useState(false);

	let json = null;
	try {
		json = JSON.parse(raw);
	} catch {}

	const str = json ? JSON.stringify(json, null, 2) : raw;

	return (
		<Box gap="md">
			<Box direction="row" gap="sm">
				<AppCopyButton value={str}>Copy</AppCopyButton>
				<Button onPress={() => setWrap(w => !w)}>
					Toggle Wrap
				</Button>
			</Box>
			<Box>
				<ScrollView horizontal={!wrap}>
					<Text selectable style={{ fontFamily: "monospace" }}>
						{str}
					</Text>
				</ScrollView>
			</Box>
		</Box>
	);
};
