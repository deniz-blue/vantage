import { Fragment, useState } from "react";
import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { AppCopyButton } from "../core/AppCopyButton";
import { Action } from "./action";
import { Sheet } from "../base/sheet/Sheet";
import { ViewRawSheetContent } from "../app/ViewRawSheet";
import { IconCode, IconCopy } from "@tabler/icons-react-native";
import { Colors } from "../../theme/colors";
import { IconSize } from "../../theme/sizing";

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
			<Button
				leftSection={action.icon}
				children={action.label}
				onPress={action.onRun}
				variant={action.danger ? "danger" : undefined}
				justify="flex-start"
			/>
		);

	if (action.type === "raw") return <RawActionButton action={action} />;

	return null;
};

export const RawActionButton = ({ action }: { action: Extract<Action, { type: "raw" }> }) => {
	const [open, setOpen] = useState(false);

	return (
		<Fragment>
			<Button
				leftSection={action.icon ?? <IconCode size={IconSize.xs} color={Colors.Text} />}
				children={action.label}
				onPress={() => setOpen(true)}
				justify="flex-start"
			/>
			<Sheet open={open} onClose={() => setOpen(false)}>
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
