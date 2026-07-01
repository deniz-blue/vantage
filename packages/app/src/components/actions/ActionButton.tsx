import { Box } from "../base/Box";
import { Button } from "../base/button/Button";
import { AppCopyButton } from "../core/AppCopyButton";
import { Action } from "./action";

export const ActionButton = ({
	action,
}: {
	action: Action;
}) => {
	if (action.type === "copy") return (
		<AppCopyButton
			value={action.value}
			leftSection={action.icon}
			children={action.label}
		/>
	);

	if (action.type === "fn") return (
		<Button
			leftSection={action.icon}
			children={action.label}
			onPress={action.onRun}
		/>
	);

	return null;
};

export const ActionButtonList = ({
	actions,
}: {
	actions: Action[];
}) => {
	return (
		<Box gap="sm">
			{actions.map((act, i) => (
				<ActionButton action={act} key={i} />
			))}
		</Box>
	)
};
