import { Linking, ScrollView } from "react-native";
import { Box } from "../../components/base/Box";
import { Text } from "../../components/base/Text";
import { Divider } from "../../components/base/Divider";
import { LanguageSelect } from "../../components/core/LanguageSelect";
import { JsonImportSheetContent } from "../../components/app/JsonImportSheet";
import { useLocaleStore } from "../../stores/useLocaleStore";
import { TimezoneSelect } from "../../components/core/timezone-select";
import { Container } from "../../components/base/Container";
import { FontSize } from "../../theme/sizing";
import { InputWrapper } from "../../components/base/input/InputWrapper";
import { Button } from "../../components/base/button/Button";
import { Colors } from "../../theme/colors";
import { IconExternalLink } from "@tabler/icons-react-native";
import { EventsManager, queryClient } from "@vantage/core";
import { AsyncButton } from "../../components/base/button/AsyncButton";
import { db, schema } from "@vantage/db";
import { SafeAreaView } from "react-native-safe-area-context";
import { ButtonSheet } from "../../components/app/ButtonSheet";
import { DiagnosticsPanel } from "../../components/app/debug/DiagnosticsPanel";
import { AtProtoSettings } from "../../components/user/AtProtoSettings";

export default function Settings() {
	const language = useLocaleStore((s) => s.language);
	const setLanguage = useLocaleStore((s) => s.setLanguage);
	const timezone = useLocaleStore((s) => s.timezone);
	const setTimezone = useLocaleStore((s) => s.setTimezone);

	return (
		<Box component={ScrollView} flex={1}>
			<Box component={SafeAreaView} flex={1}>
				<Container size="lg" py="md" gap="md">
					<Box mb="sm">
						<Text fz={FontSize.h1} fw="bold">
							Settings
						</Text>
					</Box>

					<LanguageSelect label="Content Language" value={language} onChange={setLanguage} />

					<TimezoneSelect label="Timezone" value={timezone} onChange={setTimezone} />

					<Divider my="md" />

					<AtProtoSettings />

					<InputWrapper label="App">
						<Button
							onPress={() => Linking.openURL("https://github.com/deniz-blue/vantage/issues")}
							justify="flex-start"
							rightSection={<IconExternalLink size={FontSize.xs} color={Colors.Text} />}
						>
							Bug Reports / Feedback
						</Button>
					</InputWrapper>

					<InputWrapper label="Diagnostics">
						<DiagnosticsPanel />
					</InputWrapper>

					<InputWrapper label="Developer Tools">
						<ButtonSheet sheet={<JsonImportSheetContent />}>
							<Button justify="flex-start">Import JSON</Button>
						</ButtonSheet>

						<AsyncButton
							fn={async () => {
								const links = [
									"https://deniz.blue/events-data/2025/tr-cosplay/sakura-festival.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/sanat-marketi.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/the-concastle-ii.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/slurp-serve.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/japon-k-lt-r-festivali.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/dotcon.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/bucon-25.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/cosplay-board-game.evnt.json",
									"https://deniz.blue/events-data/2025/tr-cosplay/ants-fusion.evnt.json",
									"https://deniz.blue/events-data/2026/conventions/ccb26.evnt.json",
								];
								for (let link of links)
									await EventsManager.addEvent({
										format: { type: "directory.evnt.event" },
										source: { type: "http", url: link },
									});
							}}
						>
							{({ loading, onPress }) => (
								<Button onPress={onPress} loading={loading} justify="flex-start">
									Add Test Events
								</Button>
							)}
						</AsyncButton>

						<AsyncButton
							fn={async () => {
								await db.transaction(async (tx) => {
									await tx.delete(schema.events);
									await tx.delete(schema.eventMeta);
									await tx.delete(schema.eventCache);
									await tx.delete(schema.eventTags);
								});
								await queryClient.invalidateQueries();
							}}
						>
							{({ loading, onPress }) => (
								<Button
									onLongPress={onPress}
									loading={loading}
									variant="danger"
									justify="flex-start"
								>
									Delete ALL Events (hold to confirm)
								</Button>
							)}
						</AsyncButton>
					</InputWrapper>
				</Container>
			</Box>
		</Box>
	);
}
