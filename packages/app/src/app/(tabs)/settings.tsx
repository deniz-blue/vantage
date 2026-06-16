import { useState } from "react";
import { ScrollView, TouchableOpacity } from "react-native";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Divider } from "../../components/base/Divider";
import { LanguageSelect } from "../../components/settings/LanguageSelect";
import { JsonImportSheet } from "../../components/dev/JsonImportSheet";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { Colors } from "../../theme/colors";
import { TimezoneSelect } from "../../components/core/timezone-select";
import { Container } from "../../components/base/Container";
import { FontSize } from "../../theme/sizing";

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

				<Text c={Colors.TextDimmed} fw="600">
					Developer
				</Text>

				<TouchableOpacity onPress={() => setJsonImportOpen(true)}>
					<Box
						px="md"
						py="sm"
						bg={Colors.BackgroundLight}
						radius={8}
						direction="row"
						align="center"
						gap={8}
					>
						<Text fz={FontSize.xs}>Import JSON</Text>
						<Text fz={FontSize.xs} c="TextDimmed">
							Paste an OpenEvnt event as JSON
						</Text>
					</Box>
				</TouchableOpacity>

				<JsonImportSheet
					open={jsonImportOpen}
					onClose={() => setJsonImportOpen(false)}
				/>
			</Container>
		</Box>
	);
}
