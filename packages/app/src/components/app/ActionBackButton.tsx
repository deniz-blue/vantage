import { useRouter } from "expo-router";
import { ActionIcon } from "../base/button/ActionIcon";
import { IconArrowLeft } from "@tabler/icons-react-native";
import { IconSize } from "../../theme/sizing";
import { Colors } from "../../theme/colors";

export const ActionBackButton = () => {
	const router = useRouter();

	return (
		<ActionIcon
			variant="subtle"
			onPress={() => {
				if (router.canGoBack()) router.back();
				else router.push("/");
			}}
			size="auto"
		>
			<IconArrowLeft size={IconSize.md} color={Colors.Text} />
		</ActionIcon>
	);
};
