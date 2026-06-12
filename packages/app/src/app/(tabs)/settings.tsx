import { ScrollView } from "react-native";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Divider } from "../../components/base/Divider";
import { LanguageSelect } from "../../components/settings/LanguageSelect";
import { TimezoneSelect } from "../../components/settings/TimezoneSelect";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { Colors } from "../../theme/colors";
import { Spacing } from "../../theme/spacing";

export default function Settings() {
	const language = useLocaleStore((s) => s.language);
	const setLanguage = useLocaleStore((s) => s.setLanguage);
	const timezone = useLocaleStore((s) => s.timezone);
	const setTimezone = useLocaleStore((s) => s.setTimezone);

	return (
		<ScrollView
			style={{ flex: 1, backgroundColor: Colors.Background }}
			contentContainerStyle={{ padding: Spacing.md, gap: 8 }}
		>
			<Text fz={24} fw="bold" mb="sm">
				Settings
			</Text>

			<Text fz={13} c={Colors.TextDimmed} fw="600" mt="md">
				Localization
			</Text>

			<LanguageSelect value={language} onChange={setLanguage} />

			<TimezoneSelect value={timezone} onChange={setTimezone} />

			<Divider my="md" />
		</ScrollView>
	);
}
