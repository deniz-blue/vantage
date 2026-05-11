import { LanguageSelect } from "./LanguageSelect";
import { TimezoneSelect } from "./TimezoneSelect";
import { ATProtoSettings } from "./ATProtoSettings";
import { Button, Divider, Stack } from "@mantine/core";
import { useLocaleStore } from "../../../../stores/useLocaleStore";
import { AsyncAction } from "../../../data/AsyncAction";
import { _migrate26may_ } from "../../../../db/migrations/26may";
import { schema, db } from "@vantage/db";
import { queryClient } from "@vantage/core";
import { notifications } from "@mantine/notifications";
import { sqlite } from "../../../../db/drizzle";

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
				action={async () => {
					await _migrate26may_();
					await queryClient.invalidateQueries();
				}}
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

			<AsyncAction
				action={async () => {
					console.log(await db.select().from(schema.eventCache));
				}}
			>
				{({ loading, onClick }) => (
					<Button
						onClick={onClick}
						loading={loading}
					>
						Dump Cache to Console
					</Button>
				)}
			</AsyncAction>

			<AsyncAction
				action={async () => {
					await sqlite.transaction(async tx => {
						await tx.query(db.delete(schema.eventCache));
						await tx.query(db.delete(schema.eventMeta));
						await tx.query(db.delete(schema.events));
						await tx.query(db.delete(schema.eventTags));
						await tx.query(db.delete(schema.tagHierarchy));
						await tx.query(db.delete(schema.tags));
					});
					await queryClient.invalidateQueries();
					notifications.show({ title: "Success", message: "All data has been deleted", color: "green" });
				}}
			>
				{({ loading, onClick }) => (
					<Button
						color="red"
						onClick={onClick}
						loading={loading}
					>
						Delete Everything
					</Button>
				)}
			</AsyncAction>
		</Stack>
	);
};
