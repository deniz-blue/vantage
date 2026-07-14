import type { SheetImplProps } from "./SheetImpl";
import { Modal, Pressable } from "react-native";
import { Box } from "../Box";
import { Colors } from "../../../theme/colors";
import { Radius } from "../../../theme/sizing";
import { ScrollView } from "react-native-gesture-handler";

export const SheetImpl = ({ children, open, onClose }: SheetImplProps) => {
	return (
		<Modal visible={open} onRequestClose={onClose} animationType="none" transparent>
			<Box
				component={Pressable}
				onPress={onClose}
				justify="center"
				align="center"
				flex={1}
				bg="rgba(0,0,0,0.5)"
			>
				<Box
					component={Pressable}
					onPress={(e) => e.stopPropagation()}
					bg={Colors.Background}
					radius={Radius.Default}
					w="100%"
					my="md"
					style={{
						maxWidth: 480,
						maxHeight: "90%",
						overflow: "hidden",
					}}
				>
					<ScrollView>{children}</ScrollView>
				</Box>
			</Box>
		</Modal>
	);
};
