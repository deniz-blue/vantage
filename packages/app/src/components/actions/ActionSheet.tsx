import { Sheet } from "../base/sheet/Sheet";
import { Action } from "./action";
import { ActionButtonList } from "./ActionButton";

export const ActionSheet = ({
	open,
	onClose,
	actions,
}: {
	open: boolean;
	onClose: () => void;
	actions: Action[];
}) => {
	return (
		<Sheet open={open} onClose={onClose}>
			<ActionButtonList actions={actions} />
		</Sheet>
	);
};
