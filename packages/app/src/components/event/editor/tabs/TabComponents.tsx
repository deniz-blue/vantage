import { BaseTab } from "../BaseTab";
import { EditLinkList } from "../parts/EditLinkList";
import { EditSplashMediaList } from "../parts/EditSplashMediaList";

export const TabComponents = () => {
	return (
		<BaseTab>
			<EditLinkList />
			<EditSplashMediaList />
		</BaseTab>
	);
};
