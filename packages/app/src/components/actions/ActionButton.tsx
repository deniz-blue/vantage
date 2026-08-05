import { Fragment, useRef } from "react";
import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { AppCopyButton } from "../core/AppCopyButton";
import { Action } from "./action";
import { Sheet, SheetRef } from "../base/sheet/Sheet";
import { ViewRawSheetContent } from "../app/ViewRawSheet";
import { IconCode, IconCopy, IconExternalLink } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { IconSize } from "../../theme/sizing";
import { Linking } from "react-native";
import { AsyncButton } from "../base/button/AsyncButton";

export const ActionButton = ({ action }: { action: Action }) => {
	if (action.type === "copy")
		return (
			<AppCopyButton
				justify="flex-start"
				value={action.value}
				leftSection={action.icon ?? <IconCopy size={IconSize.xs} color={Colors.Text} />}
				children={action.label}
			/>
		);

	if (action.type === "fn")
		return (
			<AsyncButton fn={async () => await action.onRun()}>
				{({ loading, onPress }) => (
					<Button
						leftSection={action.icon}
						children={action.label}
						onPress={onPress}
						loading={loading}
						variant={action.danger ? "danger" : undefined}
						justify="flex-start"
					/>
				)}
			</AsyncButton>
		);

	if (action.type === "link")
		return (
			<Button
				leftSection={action.icon ?? <IconExternalLink size={IconSize.xs} color={Colors.Text} />}
				children={action.label}
				onPress={() => Linking.openURL(action.url)}
				justify="flex-start"
			/>
		);

	if (action.type === "raw") return <RawActionButton action={action} />;

	return null;
};

export const RawActionButton = ({ action }: { action: Extract<Action, { type: "raw" }> }) => {
	const sheet = useRef<SheetRef>(null);

	return (
		<Fragment>
			<Button
				leftSection={action.icon ?? <IconCode size={IconSize.xs} color={Colors.Text} />}
				children={action.label}
				onPress={() => sheet.current?.present()}
				justify="flex-start"
			/>
			<Sheet ref={sheet}>
				<ViewRawSheetContent raw={action.value} />
			</Sheet>
		</Fragment>
	);
};

export const ActionButtonList = ({ actions }: { actions: Action[] }) => {
	return (
		<Box gap="sm">
			{actions.map((act, i) => (
				<ActionButton action={act} key={i} />
			))}
		</Box>
	);
};
