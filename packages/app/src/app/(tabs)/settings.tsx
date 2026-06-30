import { useState } from "react";
import { Linking, ScrollView, TouchableOpacity } from "react-native";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Divider } from "../../components/base/Divider";
import { LanguageSelect } from "../../components/core/LanguageSelect";
import { JsonImportSheet } from "../../components/app/JsonImportSheet";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { Colors } from "../../theme/colors";
import { TimezoneSelect } from "../../components/core/timezone-select";
import { Container } from "../../components/base/Container";
import { FontSize } from "../../theme/sizing";
import { InputWrapper } from "../../components/base/input/InputWrapper";
import { Button } from "../../components/base/button/Button";
import { IconExternalLink } from "@tabler/icons-react-native";

export default function Settings() {
	const language = useLocaleStore((s) => s.language);
	const setLanguage = useLocaleStore((s) => s.setLanguage);
	const timezone = useLocaleStore((s) => s.timezone);
	const setTimezone = useLocaleStore((s) => s.setTimezone);
	const [jsonImportOpen, setJsonImportOpen] = useState(false);

	return (
		<Box component={ScrollView} flex={1} bg={Colors.Background}>
			<Container size="lg" py="md" gap="md">
				<Box mb="sm">
					<Text fz={FontSize.h1} fw="bold">
						Settings
					</Text>
				</Box>

				<LanguageSelect label="Content Language" value={language} onChange={setLanguage} />

				<TimezoneSelect label="Timezone" value={timezone} onChange={setTimezone} />

				<Divider my="md" />

				<InputWrapper label="App">
					<Button
						onPress={() => Linking.openURL("https://github.com/deniz-blue/vantage/issues")}
						justify="flex-start"
						rightSection={<IconExternalLink size={FontSize.xs} />}
					>
						Bug Reports / Feedback
					</Button>
				</InputWrapper>

				<InputWrapper label="Developer Tools">
					<Button
						onPress={() => setJsonImportOpen(true)}
						justify="flex-start"
					>
						Import JSON
					</Button>
				</InputWrapper>

				<JsonImportSheet
					open={jsonImportOpen}
					onClose={() => setJsonImportOpen(false)}
				/>
			</Container>
		</Box>
	);
}
