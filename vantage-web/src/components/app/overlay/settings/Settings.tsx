import { LanguageSelect } from "./LanguageSelect";
import { TimezoneSelect } from "./TimezoneSelect";
import { IconExternalLink, IconSettings } from "@tabler/icons-react";
import { EVENT_REDIRECTOR_URL } from "../../../../constants";
import { ATProtoSettings } from "./ATProtoSettings";
import { Button, Divider, Stack } from "@mantine/core";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { useCacheEventsStore } from "../../../../lib/cache/useCacheEventsStore";
import { AsyncAction } from "../../../data/AsyncAction";
import { EventsGC } from "../../../../db/gc";

export const Settings = () => {
	const language = useLocaleStore((state) => state.language);
	const timezone = useLocaleStore((state) => state.timezone);

	return (
		<Stack>
			<Divider label="Localization" />

			<LanguageSelect
				value={language}
				onChange={lang => useLocaleStore.getState().setLanguage(lang)}
			/>

			<TimezoneSelect
				value={timezone}
				onChange={tz => useLocaleStore.setState({ timezone: tz })}
				description="Used for displaying event times in your local timezone"
			/>

			<Divider label="Atmosphere" />

			<ATProtoSettings />
		</Stack>
	);
};
