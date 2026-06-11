import { ScrollView } from "react-native";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Divider } from "../../components/base/Divider";
import { LanguageSelect } from "../../components/settings/LocalePicker";
import { TimezoneSelect } from "../../components/settings/TimezoneSelect";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { Colors } from "../../theme/colors";

export default function Settings() {
	const language = useLocaleStore((s) => s.language);
	const setLanguage = useLocaleStore((s) => s.setLanguage);
	const timezone = useLocaleStore((s) => s.timezone);
	const setTimezone = useLocaleStore((s) => s.setTimezone);

	return (
		<ScrollView
			style={{ flex: 1, backgroundColor: Colors.Background }}
			contentContainerStyle={{ padding: 16, gap: 8 }}
		>
			<Text style={{ fontSize: 24, fontWeight: "bold", marginBottom: 8 }}>
				Settings
			</Text>

			<Text style={{ fontSize: 13, color: Colors.TextDimmed, fontWeight: "600", marginTop: 8 }}>
				Localization
			</Text>

			<LanguageSelect value={language} onChange={setLanguage} />

			<TimezoneSelect value={timezone} onChange={setTimezone} />

			<Divider my="md" />
		</ScrollView>
	);
}
