import { LanguageSelect } from "./LanguageSelect";
import { TimezoneSelect } from "./TimezoneSelect";
import { ATProtoSettings } from "./ATProtoSettings";
import { Button, Divider, Stack } from "@mantine/core";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { AsyncAction } from "../../../data/AsyncAction";
import { _migrate26may_ } from "../../../../db/migrations/26may";

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

			<Divider label="Maintenance" />

			<AsyncAction
				action={() => _migrate26may_()}
			>
				{({ loading, onClick }) => (
					<Button
						color="red"
						onClick={onClick}
						loading={loading}
					>
						Migrate Database
					</Button>
				)}
			</AsyncAction>
		</Stack>
	);
};
