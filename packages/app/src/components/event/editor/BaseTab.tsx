import { PropsWithChildren } from "react";
import { Box } from "../../base/Box";
import { ScrollView } from "react-native-gesture-handler";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export const BaseTab = ({ children }: PropsWithChildren) => {
	const insets = useSafeAreaInsets();

	return (
		<Box component={ScrollView}>
			<Box p="md" gap="md">
				{children}
			</Box>
			<Box h={200} />
			<Box h={insets.bottom} />
		</Box>
	);
};
